const TYPES_VARIABLE_CLAUSE = ['TEXT', 'NUMBER', 'MONEY', 'DATE', 'BOOLEAN', 'TEXTAREA', 'LISTE'];
let compteurLigneClause = 0;

function escClause(s) {
    return (s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function ligneVariableHtml(v) {
    v = v || { nom: '', libelle: '', type: 'TEXT', obligatoire: true };
    const id = 'varc-' + (compteurLigneClause++);
    const existant = !!v.id;
    const options = TYPES_VARIABLE_CLAUSE.map(t => `<option value="${t}" ${t === v.type ? 'selected' : ''}>${t}</option>`).join('');
    return `
        <div class="repr-block" id="${id}" data-existant-id="${v.id || ''}">
            <div class="form-row">
                <div class="form-group">
                    <label>Nom (utilisé dans {{...}}) *</label>
                    <input type="text" class="var-nom" value="${escClause(v.nom)}" ${existant ? 'readonly' : ''}/>
                </div>
                <div class="form-group">
                    <label>Libellé affiché *</label>
                    <input type="text" class="var-libelle" value="${escClause(v.libelle)}"/>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Type</label>
                    <select class="var-type" onchange="basculerOptionsListeClause('${id}')">${options}</select>
                </div>
                <div class="form-group">
                    <label><input type="checkbox" class="var-obligatoire" ${v.obligatoire ? 'checked' : ''} style="width:auto; margin-right:0.4rem;"/>Obligatoire</label>
                </div>
            </div>
            <div class="form-group var-options-bloc" style="display:${v.type === 'LISTE' ? '' : 'none'};">
                <label>Options de la liste déroulante (une par ligne) *</label>
                <textarea class="var-options" rows="4" placeholder="Espèces&#10;MVola&#10;Orange Money">${escClause(v.options || (v.optionsListe || []).join('\n'))}</textarea>
            </div>
            ${existant ? '' : `<button type="button" class="btn btn-sm btn-danger" onclick="document.getElementById('${id}').remove()">Retirer</button>`}
        </div>`;
}

function basculerOptionsListeClause(id) {
    const bloc = document.getElementById(id);
    const estListe = bloc.querySelector('.var-type').value === 'LISTE';
    bloc.querySelector('.var-options-bloc').style.display = estListe ? '' : 'none';
}

function ajouterLigneVariable(donnees) {
    document.getElementById('listeVariables').insertAdjacentHTML('beforeend', ligneVariableHtml(donnees));
}

async function chargerClauseExistante() {
    if (CLAUSE_ID === null) return;
    const reponse = await fetch(API_CLAUSES + '/' + CLAUSE_ID);
    if (!reponse.ok) return;
    const clause = await reponse.json();
    (clause.variables || []).forEach(v => ajouterLigneVariable(v));
}

function lireVariablesClause() {
    return Array.from(document.querySelectorAll('#listeVariables .repr-block')).map((bloc, i) => {
        const idExistant = bloc.dataset.existantId;
        return {
            id: idExistant ? parseInt(idExistant, 10) : null,
            nom: bloc.querySelector('.var-nom').value.trim(),
            libelle: bloc.querySelector('.var-libelle').value.trim(),
            type: bloc.querySelector('.var-type').value,
            options: bloc.querySelector('.var-options').value.trim() || null,
            obligatoire: bloc.querySelector('.var-obligatoire').checked,
            ordre: i
        };
    });
}

async function enregistrer() {
    const bloc = document.getElementById('erreurGlobale');
    bloc.style.display = 'none';

    const donnees = {
        code: document.getElementById('code').value.trim(),
        titre: document.getElementById('titre').value.trim(),
        contenuModele: document.getElementById('contenuModele').value,
        ordreDefaut: parseInt(document.getElementById('ordreDefaut').value || '0', 10),
        actif: document.getElementById('actif').checked,
        variables: lireVariablesClause()
    };

    if (!donnees.code || !donnees.titre || !donnees.contenuModele) {
        bloc.textContent = 'Le code, le titre et le contenu sont obligatoires.';
        bloc.style.display = '';
        return;
    }

    let reponse;
    if (CLAUSE_ID === null) {
        reponse = await fetch(API_CLAUSES, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(donnees)
        });
    } else {
        reponse = await fetch(API_CLAUSES + '/' + CLAUSE_ID, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(donnees)
        });
        if (reponse.ok) {
            for (const v of donnees.variables) {
                const url = v.id
                    ? `${API_CLAUSES}/${CLAUSE_ID}/variables/${v.id}`
                    : `${API_CLAUSES}/${CLAUSE_ID}/variables`;
                const r = await fetch(url, {
                    method: v.id ? 'PUT' : 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(v)
                });
                if (!r.ok) { reponse = r; break; }
            }
        }
    }

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        bloc.textContent = erreur.erreur || Object.values(erreur).join(' / ') || 'Une erreur est survenue.';
        bloc.style.display = '';
        return;
    }

    window.location.href = URL_LISTE_CLAUSES;
}

document.addEventListener('DOMContentLoaded', chargerClauseExistante);
