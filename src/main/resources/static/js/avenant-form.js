const clausesAjoutees = []; // { idModeleClause, titre, contenuModele, variables: [{nom, valeur}] }
let clauseEnCoursDeSelection = null;

// ---------------------------------------------------------------
// Modal de selection d'une clause
// ---------------------------------------------------------------
function ouvrirModalClause() {
    document.getElementById('modalClause').classList.add('open');
    document.getElementById('modalClauseRecherche').style.display = '';
    document.getElementById('modalClauseVariables').style.display = 'none';
    document.getElementById('rechercheClause').value = '';
    rechercherClauses();
}
function fermerModalClause() {
    document.getElementById('modalClause').classList.remove('open');
}
function retourRechercheClause() {
    document.getElementById('modalClauseRecherche').style.display = '';
    document.getElementById('modalClauseVariables').style.display = 'none';
}

async function rechercherClauses() {
    const terme = document.getElementById('rechercheClause').value;
    const reponse = await fetch(API_CLAUSES + '?recherche=' + encodeURIComponent(terme));
    const clauses = await reponse.json();
    const conteneur = document.getElementById('listeClausesDisponibles');
    conteneur.innerHTML = '';
    if (clauses.length === 0) {
        conteneur.innerHTML = '<p class="muted">Aucune clause trouvée.</p>';
        return;
    }
    clauses.forEach(c => {
        const div = document.createElement('div');
        div.className = 'article-option';
        div.innerHTML = `<div class="titre">${c.titre}</div><div class="meta">${c.code}</div>`;
        div.onclick = () => selectionnerClause(c.id);
        conteneur.appendChild(div);
    });
}

async function selectionnerClause(id) {
    const reponse = await fetch(API_CLAUSES + '/' + id);
    const clause = await reponse.json();
    clauseEnCoursDeSelection = clause;

    document.getElementById('modalClauseRecherche').style.display = 'none';
    document.getElementById('modalClauseVariables').style.display = '';
    document.getElementById('modalClauseTitre').textContent = clause.titre;

    const conteneur = document.getElementById('champsVariablesClause');
    conteneur.innerHTML = '';
    (clause.variables || []).forEach(v => {
        const div = document.createElement('div');
        div.className = 'form-group';
        const typeInput = { NUMBER: 'number', MONEY: 'number', DATE: 'date' }[v.type] || 'text';
        if (v.type === 'TEXTAREA') {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><textarea rows="3" data-var-nom="${v.nom}"></textarea>`;
        } else if (v.type === 'BOOLEAN') {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><select data-var-nom="${v.nom}"><option value="Oui">Oui</option><option value="Non">Non</option></select>`;
        } else if (v.type === 'LISTE') {
            const options = (v.optionsListe || []).map(o => `<option value="${o}">${o}</option>`).join('');
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><select data-var-nom="${v.nom}"><option value="">— Choisir —</option>${options}</select>`;
        } else {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><input type="${typeInput}" data-var-nom="${v.nom}"/>`;
        }
        conteneur.appendChild(div);
    });
}

function validerAjoutClause() {
    const clause = clauseEnCoursDeSelection;
    if (!clause) return;
    const champs = document.querySelectorAll('#champsVariablesClause [data-var-nom]');
    const variables = Array.from(champs).map(c => ({ nom: c.dataset.varNom, valeur: c.value }));

    clausesAjoutees.push({
        idModeleClause: clause.id,
        titre: clause.titre,
        contenuModele: clause.contenuModele,
        variables
    });
    fermerModalClause();
    rendreClauses();
}

// Reproduit cote client la substitution {{variable}} -> valeur, pour l'apercu de la clause ajoutee
function substituerVariables(contenuModele, variables) {
    return contenuModele.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (m, nom) => {
        const v = variables.find(x => x.nom === nom);
        return v && v.valeur ? v.valeur : `[${nom}]`;
    });
}

function rendreClauses() {
    const conteneur = document.getElementById('listeClauses');
    if (clausesAjoutees.length === 0) {
        conteneur.innerHTML = '<p class="muted">Aucune clause ajoutée.</p>';
        return;
    }
    conteneur.innerHTML = '';
    clausesAjoutees.forEach((c, i) => {
        const bloc = document.createElement('div');
        bloc.className = 'article-chip';
        bloc.style.flexDirection = 'column';
        bloc.style.alignItems = 'stretch';
        const texte = substituerVariables(c.contenuModele, c.variables);
        bloc.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <span><strong>Article ${i + 1} — ${c.titre}</strong></span>
                <span class="actions">
                    <button type="button" title="Monter" onclick="deplacerClause(${i}, -1)">↑</button>
                    <button type="button" title="Descendre" onclick="deplacerClause(${i}, 1)">↓</button>
                    <button type="button" title="Supprimer" onclick="supprimerClauseAjoutee(${i})">🗑</button>
                </span>
            </div>
            <div class="muted" style="margin-top:0.3rem;">${texte}</div>`;
        conteneur.appendChild(bloc);
    });
}

function deplacerClause(index, delta) {
    const cible = index + delta;
    if (cible < 0 || cible >= clausesAjoutees.length) return;
    const tmp = clausesAjoutees[index];
    clausesAjoutees[index] = clausesAjoutees[cible];
    clausesAjoutees[cible] = tmp;
    rendreClauses();
}

function supprimerClauseAjoutee(index) {
    clausesAjoutees.splice(index, 1);
    rendreClauses();
}

// ---------------------------------------------------------------
// Soumission de l'avenant
// ---------------------------------------------------------------
async function creerAvenant() {
    const bloc = document.getElementById('erreurGlobale');
    bloc.style.display = 'none';

    const requete = {
        objet: document.getElementById('objet').value,
        dateEffet: document.getElementById('dateEffet').value || null,
        nouvelleDateFin: document.getElementById('nouvelleDateFin').value || null,
        clauses: clausesAjoutees.map(c => ({
            idModeleClause: c.idModeleClause,
            variables: c.variables
        }))
    };

    const reponse = await fetch(`${API_CONTRATS}/${CONTRAT_ID}/avenants`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requete)
    });

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        bloc.textContent = erreur.erreur || Object.values(erreur).join(' / ') || 'Une erreur est survenue.';
        bloc.style.display = '';
        window.scrollTo(0, 0);
        return;
    }
    const avenant = await reponse.json();
    window.location.href = URL_AVENANT + avenant.id;
}
