import { homeFor, redirectHomeIfLoggedIn, register } from '../auth.js';
import { $, setButtonLoading, toast } from '../ui.js';

function init() {
    const form = $('#registerForm');
    const alert = $('#authAlert');
    const button = $('#registerBtn');
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

        const username = $('#username').value.trim();
        const password = $('#password').value;
        const confirmPassword = $('#confirmPassword').value;
        const name = $('#name').value.trim();
        const idNumber = $('#idNumber').value.trim();
        const phone = $('#phone').value.trim();
        const email = $('#email').value.trim();

        if (username.length < 3 || username.length > 30) {
            showError('用户名长度应为 3~30 位');
            return;
        }
        if (password.length < 6) {
            showError('密码长度不能少于 6 位');
            return;
        }
        if (password !== confirmPassword) {
            showError('两次输入的密码不一致');
            return;
        }
        if (!name) {
            showError('请输入姓名');
            return;
        }
        if (!idNumber) {
            showError('请输入证件号');
            return;
        }

        setButtonLoading(button, true, '注册中…');
        try {
            const data = await register({
                username,
                password,
                name,
                idNumber,
                phone: phone || null,
                email: email || null
            });
            toast(`注册成功，欢迎 ${data.user.displayName}`, 'success', 1600);
            location.replace(homeFor(data.user));
        } catch (error) {
            showError(error.message);
            toast(error.message, 'error');
            setButtonLoading(button, false);
        }
    });

    form.querySelectorAll('input').forEach((input) => {
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
