document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');

    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();
        const email = document.getElementById('email').value;
        const pass = document.getElementById('password').value;

        fetch(`http://localhost:8081/login?email=${email}&pass=${pass}`)
            .then(res => res.text())
            .then(userId => {
                if (userId === "0") {
                    alert("Email sau parolă greșită!");
                } else {
                    localStorage.setItem('userId', userId);
                    window.location.href = '../demo/demo.html'; 
                }
            })
            .catch(() => alert("Eroare la conectare server!"));
    });
});