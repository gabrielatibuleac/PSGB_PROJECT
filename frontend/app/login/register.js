document.addEventListener('DOMContentLoaded', () => {
    const registerForm = document.getElementById('register-form');
    if (!registerForm) return;

    registerForm.addEventListener('submit', function(event) {
        event.preventDefault();

        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;
        const pass = document.getElementById('reg-password').value;

        fetch(`http://localhost:8081/register?name=${encodeURIComponent(name)}&email=${email}&pass=${pass}`)
            .then(res => res.text())
            .then(userId => {
                if (userId === "0") {
                    alert("Eroare la creare cont. Email-ul s-ar putea să existe deja.");
                } else {
                    alert("Cont creat cu succes!");
                    localStorage.setItem('userId', userId);
                    window.location.href = '../demo/demo.html'; 
                }
            })
            .catch(() => alert("Eroare la conectare server!"));
    });
});