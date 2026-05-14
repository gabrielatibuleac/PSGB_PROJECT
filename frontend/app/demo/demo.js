document.addEventListener('DOMContentLoaded', () => {
    const authContainer = document.getElementById('auth-container');
    const user = localStorage.getItem('userId');

    // 1. LOGIN / AVATAR LOGIC (Reparare căi)
    if (authContainer) {
        if (user) {
            // Folosim o cale care să meargă din orice folder
            authContainer.innerHTML = `<a href="../demo/pagina-contului.html"><img src="../../public/account.jpg" style="width:45px;height:45px;border-radius:50%;cursor:pointer"></a>`;
        } else {
            authContainer.innerHTML = `<a href="../login/login.html" class="login-link-btn">Login / Înregistrare</a>`;
        }
    }

    // 2. CATEGORIES DROPDOWN
    const toggleBtn = document.getElementById('categories-toggle');
    const menu = document.getElementById('categories-menu');
    if (toggleBtn) toggleBtn.onclick = () => menu.classList.toggle('show');

// În demo.js, modifică funcția createMovieCard:
window.createMovieCard = function(f) {
    const card = document.createElement('div');
    card.className = 'movie-card';
    // În demo.js, în funcția createMovieCard, modifică linia cu <img>:
card.innerHTML = `
    <img src="${f.imagine}?v=${new Date().getTime()}" class="movie-poster">
    <h3>${f.titlu}</h3>
`;
    card.onclick = () => {
        // Această cale funcționează și de pe Home și din Categorii
        const isHome = window.location.pathname.includes('demo.html');
        const target = isHome ? 'film.html' : '../demo/film.html';
        window.location.href = `${target}?id=${f.id}`;
    };
    return card;
};

    // 4. LOAD HOME MOVIES
    const homeGrid = document.getElementById('home-movies-grid');
    if (homeGrid) {
        fetch('http://localhost:8081/filme')
            .then(res => res.json())
            .then(filme => {
                homeGrid.innerHTML = "";
                filme.forEach(f => homeGrid.appendChild(createMovieCard(f)));
            });
    }

    // 5. SEARCH LOGIC
    const searchInput = document.querySelector('.search-box input');
    const container = document.getElementById('home-movies-grid') || document.getElementById('container-filme');
    if (searchInput && container) {
        searchInput.onkeypress = (e) => {
            if (e.key === 'Enter') {
                const hero = document.querySelector('.hero');
                if (hero) hero.style.display = 'none';
                fetch(`http://localhost:8081/filme?search=${searchInput.value}`)
                    .then(res => res.json())
                    .then(filme => {
                        container.innerHTML = "";
                        filme.forEach(f => container.appendChild(createMovieCard(f)));
                    });
            }
        };
    }

// 6. HEART BUTTON (LIKE) - Varianta corectată
// 6. HEART BUTTON (LIKE) - Fără alerte, doar vizual
const heartBtn = document.querySelector('.btn-heart');
if (heartBtn) {
    heartBtn.onclick = () => {
        const currentUser = localStorage.getItem('userId');
        if (!currentUser || currentUser === "0") return alert("Loghează-te!");

        fetch(`http://localhost:8081/add-to-watchlist?user=${currentUser}&movie=3`)
            .then(res => res.text())
            .then(msg => {
                // Schimbăm doar culoarea butonului, fără alert()
                if (msg === "Adaugat") {
                    heartBtn.style.background = "#ff4757"; // Roșu aprins
                } else if (msg === "Sters") {
                    heartBtn.style.background = "rgba(255, 255, 255, 0.2)"; // Transparent
                }
            })
            .catch(err => console.error("Eroare la Like:", err));
    };
}
    // --- LOGICA MODAL WATCH ---
const watchBtn = document.querySelector('.btn-watch');
const watchModal = document.getElementById('watch-modal');
const closeModal = document.querySelector('.close-modal');

if (watchBtn && watchModal) {
    // Deschide modalul la click pe Watch
    watchBtn.onclick = () => {
        watchModal.classList.add('show');
    };

    // Închide modalul la click pe X
    closeModal.onclick = () => {
        watchModal.classList.remove('show');
    };

    // Închide modalul dacă dai click oriunde pe fundalul negru
    window.onclick = (event) => {
        if (event.target == watchModal) {
            watchModal.classList.remove('show');
        }
    };
}
 const heroTitle = document.querySelector('.hero h1');
    const heroTrailer = document.querySelector('.trailer-link');
    const heroWatchBtn = document.querySelector('.btn-watch');

    if (heroTitle) {
        fetch('http://localhost:8081/movie-details?id=3')
            .then(res => res.json())
            .then(data => {
                // Setăm titlul și descrierea din DB
                heroTitle.innerText = data.titlu;
                document.querySelector('.hero p').innerText = data.descriere;
                
                // Setăm link-ul de trailer și FORȚĂM afișarea lui
                if (data.trailer && data.trailer.trim() !== "") {
                    heroTrailer.href = data.trailer;
                } else {
                    // Dacă nu are trailer în DB, va face o căutare automată pe YouTube
                    heroTrailer.href = `https://www.youtube.com/results?search_query=trailer+${encodeURIComponent(data.titlu)}`;
                }
                heroTrailer.style.display = 'flex'; // Îl facem mereu vizibil!
            });
    }

    // Butonul Watch deschide modalul existent
    if (heroWatchBtn) {
        const modal = document.getElementById('watch-modal');
        heroWatchBtn.onclick = () => modal.classList.add('show');
    }
});