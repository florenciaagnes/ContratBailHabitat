let articleEnCours = null;

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
    articleEnCours = article;

    document.getElementById('modalRecherche').style.display = 'none';
    document.getElementById('modalVariables').style.display = '';
    document.getElementById('modalArticleTitre').textContent = article.titre;

    const conteneur = document.getElementById('champsVariables');
    conteneur.innerHTML = '';
    (article.variables || []).forEach(v => {
        const div = document.createElement('div');
        div.className = 'form-group';
        const typeInput = { NUMBER: 'number', MONEY: 'number', DATE: 'date' }[v.type] || 'text';
        if (v.type === 'TEXTAREA') {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><textarea rows="3" data-var-nom="${v.nom}"></textarea>`;
        } else {
            div.innerHTML = `<label>${v.libelle}${v.obligatoire ? ' *' : ''}</label><input type="${typeInput}" data-var-nom="${v.nom}"/>`;
        }
        conteneur.appendChild(div);
    });
}

async function validerAjoutArticleExistant() {
    if (!articleEnCours) return;
    const champs = document.querySelectorAll('#champsVariables [data-var-nom]');
    const variables = Array.from(champs).map(c => ({ nom: c.dataset.varNom, valeur: c.value }));

    const reponse = await fetch(`${API_CONTRATS}/${CONTRAT_ID}/articles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idModeleArticle: articleEnCours.id, variables })
    });

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        alert(erreur.erreur || 'Impossible d\'ajouter cet article.');
        return;
    }
    window.location.reload();
}
