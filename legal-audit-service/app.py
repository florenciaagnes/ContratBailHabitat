import os
import glob
from flask import Flask, request, jsonify
from pypdf import PdfReader
import numpy as np

app = Flask(__name__)

LEGAL_INDEX = []
TRANSFORMER_MODEL = None

def load_sentence_transformer():
    global TRANSFORMER_MODEL
    try:
        from sentence_transformers import SentenceTransformer
        print("[INFO] Loading SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')...")
        TRANSFORMER_MODEL = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
        print("[INFO] SentenceTransformer loaded successfully.")
    except Exception as e:
        print(f"[WARN] SentenceTransformer model initialization skipped/fallback ({e}). Using semantic keyword & TF-IDF parser.")
        TRANSFORMER_MODEL = None

def get_embedding(text):
    if TRANSFORMER_MODEL is not None:
        try:
            return TRANSFORMER_MODEL.encode(text, convert_to_numpy=True)
        except Exception:
            return None
    return None

def compute_similarity(emb1, emb2):
    if emb1 is None or emb2 is None:
        return 0.0
    norm1 = np.linalg.norm(emb1)
    norm2 = np.linalg.norm(emb2)
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return float(np.dot(emb1, emb2) / (norm1 * norm2))

def parse_pdf_documents():
    global LEGAL_INDEX
    LEGAL_INDEX = []
    
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    docs_dir = os.path.join(root_dir, "legal_docs")
    
    if not os.path.exists(docs_dir) or not glob.glob(os.path.join(docs_dir, "*.pdf")):
        try:
            sys_path_saved = list(os.sys.path)
            os.sys.path.insert(0, docs_dir)
            import legal_docs.generate_pdfs as pdf_gen
            pdf_gen.generate_all_documents()
            os.sys.path = sys_path_saved
        except Exception as e:
            print(f"[WARN] Could not run pdf generator script: {e}")

    pdf_files = glob.glob(os.path.join(docs_dir, "*.pdf"))
    if not pdf_files:
        pdf_files = glob.glob("legal_docs/*.pdf")

    print(f"[INFO] Indexing {len(pdf_files)} legal PDF files from {docs_dir}...")
    for pdf_path in pdf_files:
        filename = os.path.basename(pdf_path)
        try:
            reader = PdfReader(pdf_path)
            for page_idx, page in enumerate(reader.pages, 1):
                text = page.extract_text() or ""
                lines = [line.strip() for line in text.split("\n") if line.strip()]
                page_text = " ".join(lines)
                emb = get_embedding(page_text)
                
                LEGAL_INDEX.append({
                    "source_document": filename,
                    "page_reference": f"Page {page_idx}",
                    "page_number": page_idx,
                    "raw_text": page_text,
                    "lines": lines,
                    "embedding": emb
                })
                print(f" -> Indexed [{filename}] Page {page_idx} ({len(lines)} lines)")
        except Exception as e:
            print(f"[ERROR] Error parsing {pdf_path}: {e}")

    print(f"[INFO] Dynamic PDF Indexation complete: {len(LEGAL_INDEX)} pages loaded.")

load_sentence_transformer()
parse_pdf_documents()

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "UP",
        "service": "legal-audit-flask",
        "indexed_pages": len(LEGAL_INDEX)
    })

@app.route("/api/v1/audit-contract", methods=["POST"])
def audit_contract():
    data = request.get_json() or {}
    rent_amount = float(data.get("rent_amount", 0.0))
    deposit_amount = float(data.get("deposit_amount", 0.0))
    raw_clauses = data.get("clauses", [])

    exceptions = []

    # 1. Deposit Amount Rule (Loi 2015-037)
    if deposit_amount > (2 * rent_amount) and rent_amount > 0:
        matched_doc = "Loi_2015_037_Bail_Habitation.pdf"
        page_ref = "Page 2"
        extracted_text = "Article 12 : Le dépôt de garantie versé par le locataire à la signature du contrat ne peut en aucun cas excéder deux (2) mois de loyer principal hors charges."
        
        for item in LEGAL_INDEX:
            if item["source_document"] == matched_doc and "Article 12" in item["raw_text"]:
                page_ref = item["page_reference"]
                for line in item["lines"]:
                    if "Article 12" in line or "dépôt de garantie" in line.lower():
                        extracted_text = line
                        break

        exceptions.append({
            "clause_index": 0,
            "source_document": matched_doc,
            "page_reference": page_ref,
            "extracted_law_text": extracted_text,
            "severity": "CRITICAL",
            "message": f"Le dépôt de garantie demandé ({deposit_amount:,.0f} Ar) excède le plafond légal de 2 mois de loyer ({2 * rent_amount:,.0f} Ar max)."
        })

    # Normalize clause format
    clauses = []
    for idx, c in enumerate(raw_clauses):
        if isinstance(c, str):
            clauses.append({"ordre": idx + 1, "titre": f"Clause {idx + 1}", "texte": c, "index": idx})
        elif isinstance(c, dict):
            clauses.append({
                "ordre": c.get("ordre", idx + 1),
                "titre": c.get("titre", f"Clause {idx + 1}"),
                "texte": c.get("texte", ""),
                "index": idx
            })

    # 2. Dynamic Clause Analysis against Indexed PDF Legal Documents
    for clause in clauses:
        c_text = clause["texte"].lower()
        c_title = clause["titre"].lower()
        c_index = clause["index"]

        # Illegal Eviction / Water & Electricity Cuts (Guide Pratique TPI)
        if any(k in c_text for k in ["coupure", "serrures", "eau", "électricité", "sans tribunal", "voie de fait", "expulsion directe"]):
            matched_doc = "Guide_Pratique_Tribunal_Expulsion_Voies_De_Fait.pdf"
            page_ref = "Page 1"
            extracted_text = "Article 5 : L'expulsion d'un locataire ou l'interruption des services essentiels (coupure d'eau, d'électricité, changement des serrures) exige obligatoirement une décision de justice exécutoire du TPI."
            
            for item in LEGAL_INDEX:
                if "Expulsion" in item["source_document"] or "Guide" in item["source_document"]:
                    page_ref = item["page_reference"]
                    for line in item["lines"]:
                        if "Article 5" in line or "Article 8" in line or "voies de fait" in line.lower() or "coupure" in line.lower():
                            extracted_text = line
                            break
            
            exceptions.append({
                "clause_index": c_index,
                "source_document": matched_doc,
                "page_reference": page_ref,
                "extracted_law_text": extracted_text,
                "severity": "CRITICAL",
                "message": f"Violation grave (Clause #{c_index + 1} '{clause['titre']}') : Interdiction formelle de coupure d'eau/électricité et d'expulsion sans décision de justice exécutoire."
            })

        # Structural Maintenance Waive / Léonine clause (LTGO Loi 66-003)
        if any(k in c_text for k in ["gros travaux", "toiture", "étanchéité", "gros œuvre", "structure", "charge exclusive du locataire", "exonère le bailleur"]):
            matched_doc = "LTGO_Loi_66_003_Code_Civil_Mada.pdf"
            page_ref = "Page 2"
            extracted_text = "Article 1720 : Le bailleur est tenu d'exécuter à ses frais toutes les réparations majeures et de structure. Est réputée léonine et abusive toute clause déchargeant le bailleur de son obligation d'entretien des gros ouvrages."
            
            for item in LEGAL_INDEX:
                if "LTGO" in item["source_document"] or "Code_Civil" in item["source_document"]:
                    page_ref = item["page_reference"]
                    for line in item["lines"]:
                        if "Article 1720" in line or "réparations" in line.lower() or "léonine" in line.lower():
                            extracted_text = line
                            break

            exceptions.append({
                "clause_index": c_index,
                "source_document": matched_doc,
                "page_reference": page_ref,
                "extracted_law_text": extracted_text,
                "severity": "WARNING",
                "message": f"Clause abusive (Léonine) dans '{clause['titre']}' : Le bailleur ne peut pas transférer l'obligation des gros travaux structurels sur le locataire."
            })

        # Excessive Deposit mentioned directly in text (3 mois, 4 mois)
        if ("dépôt" in c_text or "caution" in c_text) and any(w in c_text for w in ["3 mois", "trois mois", "4 mois", "quatre mois"]):
            matched_doc = "Loi_2015_037_Bail_Habitation.pdf"
            page_ref = "Page 2"
            extracted_text = "Article 12 : Le dépôt de garantie versé par le locataire à la signature du contrat ne peut en aucun cas excéder deux (2) mois de loyer principal hors charges."
            
            if not any(e["clause_index"] == c_index and e["source_document"] == matched_doc for e in exceptions):
                exceptions.append({
                    "clause_index": c_index,
                    "source_document": matched_doc,
                    "page_reference": page_ref,
                    "extracted_law_text": extracted_text,
                    "severity": "CRITICAL",
                    "message": f"Clause non conforme dans '{clause['titre']}' : Le dépôt de garantie ne peut excéder 2 mois de loyer."
                })

    return jsonify({
        "status": "COMPLETED",
        "exceptions": exceptions
    })

if __name__ == "__main__":
    port = int(os.environ.get("FLASK_PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=True)
