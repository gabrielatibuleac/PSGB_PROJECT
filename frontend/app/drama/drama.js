document.addEventListener("DOMContentLoaded", () => {
    const provider = document.getElementById('category-id-provider');
    const container = document.getElementById('container-filme');

    if (!provider || !container) return;

    const catId = provider.getAttribute('data-id');

    fetch(`http://localhost:8081/filme?cat=${catId}`)
        .then(res => res.json())
        .then(filme => {
            container.innerHTML = ""; 
filme.forEach(f => {
    const card = document.createElement('div');
    card.className = 'movie-card';
    card.innerHTML = `
        <img src="${f.imagine}" class="movie-poster">
        <h3>${f.titlu}</h3>
        <p>${f.an}</p>
    `;
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