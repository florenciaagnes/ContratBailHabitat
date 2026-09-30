// Modal de renouvellement par tacite reconduction, reutilise sur
// contrats/liste.html, contrats/archives.html et contrats/voir.html.
// Chaque page doit definir API_CONTRATS et URL_CONTRAT (prefixe "/contrats/").
let contratARenouvelerId = null;

// Delegation d'evenement : les boutons ".renouveler-btn" portent leurs donnees
// dans des attributs data-* (Thymeleaf interdit l'interpolation de chaines
// dans les attributs d'evenements comme onclick, pour eviter les injections).
document.addEventListener('click', (evenement) => {
    const bouton = evenement.target.closest('.renouveler-btn');
    if (bouton) {
        ouvrirModalRenouvellement(parseInt(bouton.dataset.id, 10), bouton.dataset.numero, bouton.dataset.dateFin || null);
    }
});

function formatDateAffichage(iso) {
    if (!iso) return '—';
    const [a, m, j] = iso.split('-');
    return `${j}/${m}/${a}`;
}

function ajouterUnJour(iso) {
    const d = new Date(iso + 'T00:00:00');
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
}

function ouvrirModalRenouvellement(id, numero, ancienneDateFin) {
    contratARenouvelerId = id;
    document.getElementById('renouvellementNumero').textContent = numero;
    document.getElementById('renouvellementErreur').style.display = 'none';

    const champDate = document.getElementById('renouvellementDateFin');
    champDate.value = '';

    document.getElementById('renouvellementAncienneFin').textContent = formatDateAffichage(ancienneDateFin);

    if (ancienneDateFin) {
        const nouvelleDebut = ajouterUnJour(ancienneDateFin);
        document.getElementById('renouvellementNouvelleDebut').textContent = formatDateAffichage(nouvelleDebut);
        champDate.min = nouvelleDebut;
        document.getElementById('renouvellementAide').textContent =
            `Doit être postérieure au ${formatDateAffichage(ancienneDateFin)}.`;
    } else {
        document.getElementById('renouvellementNouvelleDebut').textContent = '—';
        champDate.removeAttribute('min');
        document.getElementById('renouvellementAide').textContent = '';
    }

    document.getElementById('modalRenouvellement').classList.add('open');
}

function fermerModalRenouvellement() {
    document.getElementById('modalRenouvellement').classList.remove('open');
    contratARenouvelerId = null;
}

async function confirmerRenouvellement() {
    const bloc = document.getElementById('renouvellementErreur');
    bloc.style.display = 'none';

    const nouvelleDateFin = document.getElementById('renouvellementDateFin').value;
    if (!nouvelleDateFin) {
        bloc.textContent = 'La nouvelle date de fin est obligatoire.';
        bloc.style.display = '';
        return;
    }

    const reponse = await fetch(`${API_CONTRATS}/${contratARenouvelerId}/renouveler`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nouvelleDateFin })
    });

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        bloc.textContent = erreur.erreur || Object.values(erreur).join(' / ') || 'Une erreur est survenue.';
        bloc.style.display = '';
        return;
    }

    const nouveauContrat = await reponse.json();
    window.location.href = URL_CONTRAT + nouveauContrat.id;
}
