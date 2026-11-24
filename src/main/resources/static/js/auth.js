import { api } from './api.js';
import {
    clearSession,
    getSession,
    homeFor,
    setSession
} from './session.js';

export async function login(username, password) {
    const data = await api('/auth/login', {
        method: 'POST',
        body: { username, password },
        auth: false
    });
    setSession(data);
    return data;
}

export async function register(payload) {
    const data = await api('/auth/register', {
        method: 'POST',
        body: payload,
        auth: false
    });
    setSession(data);
    return data;
}

export async function logout() {
    try {
        await api('/auth/logout', { method: 'POST' });
    } catch (error) {
        // Token 已失效时也继续清理本地会话
    }
    clearSession();
    location.replace('login.html');
}

export function currentUser() {
    return getSession()?.user || null;
}

/** 已登录时跳转到角色首页，返回是否发生了跳转 */
export function redirectHomeIfLoggedIn() {
    const session = getSession();
    if (session?.token && session.user) {
        location.replace(homeFor(session.user));
        return true;
    }
    return false;
}

export { homeFor };
