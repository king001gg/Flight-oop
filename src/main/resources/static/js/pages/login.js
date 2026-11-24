import { homeFor, login, redirectHomeIfLoggedIn } from '../auth.js';
import { $, setButtonLoading, toast } from '../ui.js';

function init() {
    const form = $('#loginForm');
    const alert = $('#authAlert');
    const button = $('#loginBtn');
    const username = $('#username');
    const password = $('#password');

    if (!form) {
        return;
    }

    const showError = (message) => {
        alert.textContent = message;
        alert.classList.add('show');
    };
    const clearError = () => alert.classList.remove('show');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearError();

        const name = username.value.trim();
        const pass = password.value;
        if (!name || !pass) {
            showError('请输入用户名和密码');
            return;
        }

        setButtonLoading(button, true, '登录中…');
        try {
            const data = await login(name, pass);
            toast(`欢迎回来，${data.user.displayName}`, 'success', 1600);
            location.replace(homeFor(data.user));
        } catch (error) {
            showError(error.message);
            toast(error.message, 'error');
            setButtonLoading(button, false);
        }
    });

    document.querySelectorAll('.demo-account').forEach((demo) => {
        demo.addEventListener('click', () => {
            username.value = demo.dataset.user || '';
            password.value = demo.dataset.pass || '';
            clearError();
            button.focus();
        });
    });

    [username, password].forEach((input) => {
        input.addEventListener('input', clearError);
    });
}

if (!redirectHomeIfLoggedIn()) {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}
