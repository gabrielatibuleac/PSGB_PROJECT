document.addEventListener("DOMContentLoaded", () => {
    // Luam ID-ul categoriei dintr-un element ascuns in HTML
    const categoryElement = document.getElementById('category-id-provider');
    if (!categoryElement) return;

    const catId = categoryElement.getAttribute('data-id');

    fetch(`http://localhost:8081/filme?cat=${catId}`)
        .then(res => res.json())
        .then(filme => {
            const container = document.getElementById('container-filme');
            if (filme.length === 0) {
                container.innerHTML = "<p class='no-movies'>Momentan nu avem filme aici.</p>";
                return;
            }

            container.innerHTML = filme.map(f => `
                <div class="movie-card">
                    <div class="poster-placeholder" style="background: linear-gradient(45deg, #1a1a1a, #333); height: 250px; display: flex; align-items: center; justify-content: center; border-radius: 12px; color: #fff; font-weight: bold; text-shadow: 1px 1px 3px #000; padding: 15px; text-align: center;">
                        ${f.titlu}
                    </div>
                    <div class="movie-info" style="padding: 10px 0;">
                        <h3 style="margin: 0; font-size: 1.1rem;">${f.titlu}</h3>
                        <p style="color: #888; font-size: 0.9rem; margin-top: 5px;">An lansare: ${f.an}</p>
                    </div>
                </div>
            `).join('');
        })
        .catch(err => console.error("Serverul Java nu raspunde:", err));
});