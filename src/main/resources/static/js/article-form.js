const TYPES_VARIABLE = ['TEXT', 'NUMBER', 'MONEY', 'DATE', 'BOOLEAN', 'TEXTAREA'];
let compteurLigne = 0;

function ligneVariableHtml(v) {
    v = v || { nom: '', libelle: '', type: 'TEXT', obligatoire: true, ordre: compteurLigne };
    const id = 'var-' + (compteurLigne++);
    const options = TYPES_VARIABLE.map(t => `<option value="${t}" ${t === v.type ? 'selected' : ''}>${t}</option>`).join('');
    return `
        <div class="repr-block" id="${id}" data-existant-id="${v.id || ''}">
            <div class="form-row">
                <div class="form-group">
                    <label>Nom (utilisé dans {{...}}) *</label>
                    <input type="text" class="var-nom" value="${v.nom || ''}"/>
                </div>
                <div class="form-group">
                    <label>Libellé affiché *</label>
                    <input type="text" class="var-libelle" value="${v.libelle || ''}"/>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Type</label>
                    <select class="var-type">${options}</select>
                </div>
                <div class="form-group">
                    <label><input type="checkbox" class="var-obligatoire" ${v.obligatoire ? 'checked' : ''} style="width:auto; margin-right:0.4rem;"/>Obligatoire</label>
                </div>
            </div>
            <button type="button" class="btn btn-sm btn-danger" onclick="document.getElementById('${id}').remove()">Retirer</button>
        </div>`;
}

function ajouterLigneVariable(donnees) {
    document.getElementById('listeVariables').insertAdjacentHTML('beforeend', ligneVariableHtml(donnees));
}

async function chargerArticleExistant() {
    if (ARTICLE_ID === null) return;
    const reponse = await fetch(API_ARTICLES + '/' + ARTICLE_ID);
    if (!reponse.ok) return;
    const article = await reponse.json();
    (article.variables || []).forEach(v => ajouterLigneVariable(v));
}

function lireVariables() {
    return Array.from(document.querySelectorAll('#listeVariables .repr-block')).map((bloc, i) => {
        const idExistant = bloc.dataset.existantId;
        return {
            id: idExistant ? parseInt(idExistant, 10) : null,
            nom: bloc.querySelector('.var-nom').value.trim(),
            libelle: bloc.querySelector('.var-libelle').value.trim(),
            type: bloc.querySelector('.var-type').value,
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
        obligatoire: document.getElementById('obligatoire').checked,
        actif: document.getElementById('actif').checked,
        variables: lireVariables()
    };

    if (!donnees.code || !donnees.titre || !donnees.contenuModele) {
        bloc.textContent = 'Le code, le titre et le contenu sont obligatoires.';
        bloc.style.display = '';
        return;
    }

    let reponse;
    if (ARTICLE_ID === null) {
        reponse = await fetch(API_ARTICLES, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(donnees)
        });
    } else {
        reponse = await fetch(API_ARTICLES + '/' + ARTICLE_ID, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(donnees)
        });
        // Les variables sont gerees separement en modification (ajout un par un)
        if (reponse.ok) {
            for (const v of donnees.variables) {
                if (!v.id) {
                    await fetch(API_ARTICLES + '/' + ARTICLE_ID + '/variables', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(v)
                    });
                }
            }
        }
    }

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        bloc.textContent = erreur.erreur || Object.values(erreur).join(' / ') || 'Une erreur est survenue.';
        bloc.style.display = '';
        return;
    }

    window.location.href = URL_LISTE_ARTICLES;
}

document.addEventListener('DOMContentLoaded', chargerArticleExistant);
