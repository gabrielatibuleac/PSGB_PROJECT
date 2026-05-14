document.addEventListener("DOMContentLoaded", () => {
    const provider = document.getElementById('category-id-provider');
    const container = document.getElementById('container-filme');

    if (!provider || !container) return;

    const catId = provider.getAttribute('data-id');

    // Apelăm Backend-ul Java
    fetch(`http://localhost:8081/filme?cat=${catId}`)
        .then(res => res.json())
        .then(filme => {
            container.innerHTML = ""; 

      // În drama.js (dacă îl mai folosești separat), la bucla forEach a filmelor:
filme.forEach(f => {
    const card = document.createElement('div');
    card.className = 'movie-card';
    card.innerHTML = `
        <img src="${f.imagine}" class="movie-poster">
        <h3>${f.titlu}</h3>
        <p>${f.an}</p>
    `;
    
    // Adaugă click-ul:
    card.onclick = () => {
        window.location.href = `../demo/film.html?id=${f.id}`;
    };
    
    container.appendChild(card);
});
        })
        .catch(err => {
            console.error("Eroare:", err);
            container.innerHTML = "<p style='color: red;'>Eroare: Asigură-te că App.java rulează!</p>";
        });
});