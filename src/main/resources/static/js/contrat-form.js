// Etat client du formulaire de creation de contrat
const etat = {
    typeLocataire: 'PERSONNE',
    representants: [],       // { nom, prenom, cin, dateCin, lieuCin, adresse, fonction }
    articlesAjoutes: [],     // { idModeleArticle, code, titre, contenuModele, variables: [{idVariable, nom, libelle, valeur}] }
    articleEnCoursDeSelection: null
};

// ---------------------------------------------------------------
// Bascule personne physique / societe pour le locataire
// ---------------------------------------------------------------
function basculerTypeLocataire(type) {
    etat.typeLocataire = type;
    document.querySelectorAll('#blocLocataire .toggle-group button').forEach(b => {
        b.classList.toggle('active', b.dataset.type === type);
    });
    document.getElementById('locatairePersonne').style.display = type === 'PERSONNE' ? '' : 'none';
    document.getElementById('locataireOrganisation').style.display = type === 'ORGANISATION' ? '' : 'none';
    mettreAJourApercu();
}

function ajouterRepresentant() {
    etat.representants.push({ nom: '', prenom: '', cin: '', dateCin: '', lieuCin: '', adresse: '', fonction: '' });
    rendreRepresentants();
}

function supprimerRepresentant(index) {
    etat.representants.splice(index, 1);
    rendreRepresentants();
}

function rendreRepresentants() {
    const conteneur = document.getElementById('listeRepresentants');
    conteneur.innerHTML = '';
    etat.representants.forEach((r, i) => {
        const bloc = document.createElement('div');
        bloc.className = 'repr-block';
        bloc.innerHTML = `
            <div class="form-row">
                <div class="form-group"><label>Nom *</label><input type="text" value="${r.nom}" oninput="etat.representants[${i}].nom=this.value; mettreAJourApercu();"/></div>
                <div class="form-group"><label>Prénom</label><input type="text" value="${r.prenom}" oninput="etat.representants[${i}].prenom=this.value; mettreAJourApercu();"/></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>CIN *</label><input type="text" value="${r.cin}" oninput="etat.representants[${i}].cin=this.value; mettreAJourApercu();"/></div>
                <div class="form-group"><label>Délivrée le</label><input type="date" value="${r.dateCin}" oninput="etat.representants[${i}].dateCin=this.value;"/></div>
                <div class="form-group"><label>À</label><input type="text" value="${r.lieuCin}" oninput="etat.representants[${i}].lieuCin=this.value;"/></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Adresse</label><input type="text" value="${r.adresse}" oninput="etat.representants[${i}].adresse=this.value;"/></div>
                <div class="form-group"><label>Fonction</label><input type="text" value="${r.fonction}" placeholder="Cogérant..." oninput="etat.representants[${i}].fonction=this.value; mettreAJourApercu();"/></div>
            </div>
            <button type="button" class="btn btn-sm btn-danger" onclick="supprimerRepresentant(${i})">Retirer</button>
        `;
        conteneur.appendChild(bloc);
    });
    mettreAJourApercu();
}

// ---------------------------------------------------------------
// Modal d'ajout d'article
// ---------------------------------------------------------------
function ouvrirModalArticle() {
    document.getElementById('modalArticle').classList.add('open');
    document.getElementById('modalRecherche').style.display = '';
    document.getElementById('modalVariables').style.display = 'none';
    document.getElementById('rechercheArticle').value = '';
    rechercherArticles();
}
function fermerModalArticle() {
    document.getElementById('modalArticle').classList.remove('open');
}
function retourRechercheArticle() {
    document.getElementById('modalRecherche').style.display = '';
    document.getElementById('modalVariables').style.display = 'none';
}

async function rechercherArticles() {
    const terme = document.getElementById('rechercheArticle').value;
    const reponse = await fetch(API_ARTICLES + '?recherche=' + encodeURIComponent(terme));
    const articles = await reponse.json();
    const conteneur = document.getElementById('listeArticlesDisponibles');
    conteneur.innerHTML = '';
    if (articles.length === 0) {
        conteneur.innerHTML = '<p class="muted">Aucun article trouvé.</p>';
        return;
    }
    articles.forEach(a => {
        const div = document.createElement('div');
        div.className = 'article-option';
        div.innerHTML = `<div class="titre">${a.titre}</div><div class="meta">${a.code} ${a.obligatoire ? '· obligatoire' : '· facultatif'}</div>`;
        div.onclick = () => selectionnerArticle(a.id);
        conteneur.appendChild(div);
    });
}

async function selectionnerArticle(id) {
    const reponse = await fetch(API_ARTICLES + '/' + id);
    const article = await reponse.json();
    etat.articleEnCoursDeSelection = article;

    document.getElementById('modalRecherche').style.display = 'none';
    document.getElementById('modalVariables').style.display = '';
    document.getElementById('modalArticleTitre').textContent = article.titre;

    const conteneur = document.getElementById('champsVariables');
    conteneur.innerHTML = '';
    (article.variables || []).forEach(v => {
        if (v.valeurAuto) return; // reprise automatique du contrat (dates)
        const div = document.createElement('div');
        div.className = 'form-group';
        const typeInput = { NUMBER: 'number', MONEY: 'number', DATE: 'date' }[v.type] || 'text';
        if (v.type === 'TEXTAREA') {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><textarea rows="3" data-var-id="${v.id}" data-var-nom="${v.nom}"></textarea>`;
        } else if (v.type === 'BOOLEAN') {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><select data-var-id="${v.id}" data-var-nom="${v.nom}"><option value="Oui">Oui</option><option value="Non">Non</option></select>`;
        } else if (v.type === 'LISTE') {
            const options = (v.optionsListe || []).map(o => `<option value="${o}">${o}</option>`).join('');
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><select data-var-id="${v.id}" data-var-nom="${v.nom}"><option value="">— Choisir —</option>${options}</select>`;
        } else {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><input type="${typeInput}" data-var-id="${v.id}" data-var-nom="${v.nom}"/>`;
        }
        conteneur.appendChild(div);
    });
}

function validerAjoutArticle() {
    const article = etat.articleEnCoursDeSelection;
    if (!article) return;
    const champs = document.querySelectorAll('#champsVariables [data-var-id]');
    const variables = [];
    for (const champ of champs) {
        const obligatoireLabel = champ.previousElementSibling ? false : false;
        variables.push({
            idVariable: parseInt(champ.dataset.varId, 10),
            nom: champ.dataset.varNom,
            valeur: champ.value
        });
    }
    etat.articlesAjoutes.push({
        idModeleArticle: article.id,
        code: article.code,
        titre: article.titre,
        contenuModele: article.contenuModele,
        variables,
        autos: (article.variables || []).filter(v => v.valeurAuto).map(v => ({ nom: v.nom, source: v.valeurAuto }))
    });
    fermerModalArticle();
    rendreArticlesAjoutes();
}

function rendreArticlesAjoutes() {
    const conteneur = document.getElementById('listeArticlesAjoutes');
    if (etat.articlesAjoutes.length === 0) {
        conteneur.innerHTML = '<p class="muted">Aucun article ajouté.</p>';
        mettreAJourApercu();
        return;
    }
    conteneur.innerHTML = '';
    etat.articlesAjoutes.forEach((a, i) => {
        const div = document.createElement('div');
        div.className = 'article-chip';
        div.innerHTML = `
            <span>Article ${i + 1} — ${a.titre}</span>
            <span class="actions">
                <button type="button" title="Monter" onclick="deplacerArticle(${i}, -1)">↑</button>
                <button type="button" title="Descendre" onclick="deplacerArticle(${i}, 1)">↓</button>
                <button type="button" title="Supprimer" onclick="supprimerArticleAjoute(${i})">🗑</button>
            </span>`;
        conteneur.appendChild(div);
    });
    mettreAJourApercu();
}

function deplacerArticle(index, delta) {
    const cible = index + delta;
    if (cible < 0 || cible >= etat.articlesAjoutes.length) return;
    const tmp = etat.articlesAjoutes[index];
    etat.articlesAjoutes[index] = etat.articlesAjoutes[cible];
    etat.articlesAjoutes[cible] = tmp;
    rendreArticlesAjoutes();
}

function supprimerArticleAjoute(index) {
    etat.articlesAjoutes.splice(index, 1);
    rendreArticlesAjoutes();
}

// Reproduit cote client la substitution {{variable}} -> valeur (voir VariableSubstitutionUtil cote serveur)
function substituerVariables(contenuModele, variables) {
    return contenuModele.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (m, nom) => {
        const v = variables.find(x => x.nom === nom);
        return v && v.valeur ? v.valeur : `[${nom}]`;
    });
}

// ---------------------------------------------------------------
// Apercu en temps reel
// ---------------------------------------------------------------
function texteOuPlaceholder(valeur, placeholder) {
    return valeur && valeur.trim() !== '' ? escapeHtml(valeur) : `<span class="placeholder">${placeholder}</span>`;
}
function escapeHtml(s) {
    return (s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function mettreAJourApercu() {
    const propNom = [document.getElementById('propNom').value, document.getElementById('propPrenom').value].filter(Boolean).join(' ');
    const propCin = document.getElementById('propCin').value;

    let blocLocataire = '';
    if (etat.typeLocataire === 'PERSONNE') {
        const nom = [document.getElementById('locNom').value, document.getElementById('locPrenom').value].filter(Boolean).join(' ');
        const cin = document.getElementById('locCin').value;
        blocLocataire = `<p>La société / Monsieur/Madame : ${texteOuPlaceholder(nom, '.......................')}<br/>
            CIN N° : ${texteOuPlaceholder(cin, '.......................')}<br/>d'autre part</p>`;
    } else {
        const nomOrg = document.getElementById('orgNom').value;
        let representantsHtml = etat.representants.map(r =>
            `&nbsp;&nbsp;• Monsieur/Madame ${texteOuPlaceholder([r.nom, r.prenom].filter(Boolean).join(' '), '...')}
             ${r.fonction ? ' - ' + escapeHtml(r.fonction) : ''}<br/>
             &nbsp;&nbsp;CIN N° : ${texteOuPlaceholder(r.cin, '...')}<br/>`
        ).join('');
        blocLocataire = `<p>La société : ${texteOuPlaceholder(nomOrg, '.......................')}<br/>
            représentée par ses représentants statutaires :<br/>${representantsHtml || '<span class="placeholder">(aucun représentant ajouté)</span>'}
            <br/>d'autre part</p>`;
    }

    let articlesHtml = etat.articlesAjoutes.map((a, i) => {
        const texte = substituerVariables(a.contenuModele, a.variables.concat((a.autos || []).map(x => ({ nom: x.nom, valeur: valeurAutomatique(x.source) }))));
        return `<div class="article-title">Article ${i + 1} : ${a.titre}</div><div>${escapeHtml(texte)}</div>`;
    }).join('');
    if (etat.articlesAjoutes.length === 0) {
        articlesHtml = '<p class="placeholder">Aucun article ajouté pour le moment.</p>';
    }

    document.getElementById('apercu').innerHTML = `
        <h2>CONTRAT DE BAIL</h2>
        <p>Entre</p>
        <p>Monsieur/Madame : ${texteOuPlaceholder(propNom, '.......................')}<br/>
        CIN N° : ${texteOuPlaceholder(propCin, '.......................')}<br/>
        PROPRIETAIRE<br/>d'une part</p>
        ${blocLocataire}
        <p><strong>II/ Il a été convenu le présent contrat de bail stipulant :</strong></p>
        ${articlesHtml}
        <div class="signatures">
            <div><strong>Le Propriétaire</strong><br/><br/><br/>Signature</div>
            <div><strong>Le Locataire</strong><br/><br/><br/>Signature</div>
        </div>
    `;
}

// Ecoute des champs simples pour mise a jour de l'apercu
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('#formulaire input, #formulaire textarea').forEach(el => {
        el.addEventListener('input', mettreAJourApercu);
    });
    mettreAJourApercu();
});

// ---------------------------------------------------------------
// Soumission du contrat
// ---------------------------------------------------------------
function construirePersonneDto(prefix) {
    return {
        nom: val(prefix + 'Nom'),
        prenom: val(prefix + 'Prenom'),
        cin: val(prefix + 'Cin'),
        dateDelivranceCin: val(prefix + 'DateCin') || null,
        lieuDelivranceCin: val(prefix + 'LieuCin'),
        adresse: val(prefix + 'Adresse')
    };
}
function val(id) {
    const el = document.getElementById(id);
    return el ? el.value : '';
}

async function creerContrat() {
    document.getElementById('erreurGlobale').style.display = 'none';

    // Validation du CIN malgache avant envoi (le serveur revalide de toute facon)
    const verifs = [['du propriétaire', val('propCin')]];
    if (etat.typeLocataire === 'PERSONNE') {
        verifs.push(['du locataire', val('locCin')]);
    } else {
        etat.representants.forEach(r => verifs.push(['du représentant ' + (r.nom || ''), r.cin]));
    }
    for (const [libelle, cin] of verifs) {
        const message = validerCin(cin, libelle);
        if (message) {
            const bloc = document.getElementById('erreurGlobale');
            bloc.textContent = message;
            bloc.style.display = '';
            window.scrollTo(0, 0);
            return;
        }
    }

    const proprietaire = { typePartie: 'PERSONNE', personne: construirePersonneDto('prop') };

    let locataire;
    if (etat.typeLocataire === 'PERSONNE') {
        locataire = { typePartie: 'PERSONNE', personne: construirePersonneDto('loc') };
    } else {
        locataire = {
            typePartie: 'ORGANISATION',
            organisation: {
                nom: val('orgNom'),
                formeJuridique: val('orgForme'),
                siegeSocial: val('orgSiege'),
                numeroIdentification: val('orgNumIdent'),
                representants: etat.representants.map(r => ({
                    fonction: r.fonction,
                    personne: { nom: r.nom, prenom: r.prenom, cin: r.cin, dateDelivranceCin: r.dateCin || null, lieuDelivranceCin: r.lieuCin, adresse: r.adresse }
                }))
            }
        };
    }

    const requete = {
        dateDebut: val('dateDebut') || null,
        dateFin: val('dateFin') || null,
        bien: {
            adresse: val('bienAdresse'),
            typeBien: val('bienType'),
            surface: val('bienSurface') || null,
            description: val('bienDescription')
        },
        proprietaire,
        locataire,
        articles: etat.articlesAjoutes.map(a => ({
            idModeleArticle: a.idModeleArticle,
            variables: a.variables
        }))
    };

    const reponse = await fetch(API_CONTRATS, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requete)
    });

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        const message = erreur.erreur || Object.values(erreur).join(' / ') || 'Une erreur est survenue.';
        const bloc = document.getElementById('erreurGlobale');
        bloc.textContent = message;
        bloc.style.display = '';
        window.scrollTo(0, 0);
        return;
    }

    const contrat = await reponse.json();
    window.location.href = URL_VOIR_CONTRAT + contrat.id;
}

// CIN malgache : 12 chiffres, 6e chiffre = 1 (homme) ou 2 (femme)
function validerCin(cin, libelle) {
    const n = (cin || '').replace(/[\s.\-]/g, '');
    if (n === '') return `Le CIN ${libelle} est obligatoire.`;
    if (!/^[0-9]+$/.test(n)) return `Le CIN ${libelle} ne doit contenir que des chiffres.`;
    if (n.length !== 12) return `Le CIN ${libelle} doit contenir exactement 12 chiffres (${n.length} saisis).`;
    if (n[5] !== '1' && n[5] !== '2') return `Le CIN ${libelle} est invalide : le 6e chiffre doit être 1 (homme) ou 2 (femme).`;
    return null;
}

// Valeurs reprises automatiquement de la section "Duree & bien loue"
function formatDateFr(iso) {
    if (!iso) return '';
    const [a, m, j] = iso.split('-');
    return `${j}/${m}/${a}`;
}
function valeurAutomatique(source) {
    return formatDateFr(val(source === 'DATE_DEBUT' ? 'dateDebut' : 'dateFin'));
}
