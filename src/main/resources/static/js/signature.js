let canvas, ctx, dessineEnCours = false;

function initCanvasSignature() {
    canvas = document.getElementById('canvasSignature');
    if (!canvas) return;
    ctx = canvas.getContext('2d');
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1f2933';

    const positionDepuisEvenement = (evt) => {
        const rect = canvas.getBoundingClientRect();
        const point = evt.touches ? evt.touches[0] : evt;
        return {
            x: (point.clientX - rect.left) * (canvas.width / rect.width),
            y: (point.clientY - rect.top) * (canvas.height / rect.height)
        };
    };

    const demarrer = (evt) => {
        evt.preventDefault();
        dessineEnCours = true;
        const p = positionDepuisEvenement(evt);
        ctx.beginPath();
        ctx.moveTo(p.x, p.y);
    };
    const dessiner = (evt) => {
        if (!dessineEnCours) return;
        evt.preventDefault();
        const p = positionDepuisEvenement(evt);
        ctx.lineTo(p.x, p.y);
        ctx.stroke();
    };
    const arreter = () => { dessineEnCours = false; };

    canvas.addEventListener('mousedown', demarrer);
    canvas.addEventListener('mousemove', dessiner);
    canvas.addEventListener('mouseup', arreter);
    canvas.addEventListener('mouseleave', arreter);
    canvas.addEventListener('touchstart', demarrer, { passive: false });
    canvas.addEventListener('touchmove', dessiner, { passive: false });
    canvas.addEventListener('touchend', arreter);
}

function ouvrirModalSignature() {
    document.getElementById('modalSignature').classList.add('open');
    setTimeout(() => {
        initCanvasSignature();
        effacerSignature();
    }, 0);
}
function fermerModalSignature() {
    document.getElementById('modalSignature').classList.remove('open');
}
function effacerSignature() {
    if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
}

async function validerSignature() {
    const idPersonne = document.getElementById('selectSignataire').value;
    if (!idPersonne) {
        alert('Sélectionnez un signataire.');
        return;
    }
    const donnees = canvas.toDataURL('image/png');
    // Verifie qu'un trait a bien ete dessine (canvas non vide)
    const vide = document.createElement('canvas');
    vide.width = canvas.width; vide.height = canvas.height;
    if (donnees === vide.toDataURL('image/png')) {
        alert('Veuillez dessiner une signature avant de valider.');
        return;
    }

    const reponse = await fetch(`${API_CONTRATS}/${CONTRAT_ID}/signatures`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idPersonne: parseInt(idPersonne, 10), signatureData: donnees })
    });

    if (!reponse.ok) {
        const erreur = await reponse.json().catch(() => ({}));
        alert(erreur.erreur || 'Impossible d\'enregistrer la signature.');
        return;
    }
    window.location.reload();
}
