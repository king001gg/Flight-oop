import { clearSession, getToken } from './session.js';

const BASE = '/api';

export class ApiError extends Error {
    constructor(message, status, data) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
    }
}

/**
 * 统一 fetch 封装：自动附加 Bearer Token、JSON、401 跳转。
 */
export async function api(path, options = {}) {
    const {
        method = 'GET',
        body = null,
        params = null,
        auth = true
    } = options;

    const url = new URL(BASE + path, location.origin);
    if (params) {
        for (const [key, value] of Object.entries(params)) {
            if (value !== undefined && value !== null && value !== '') {
                url.searchParams.set(key, value);
            }
        }
    }

    const headers = {};
    if (auth) {
        const token = getToken();
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
    }
    if (body !== null) {
        headers['Content-Type'] = 'application/json';
    }

    let response;
    try {
        response = await fetch(url, {
            method,
            headers,
            body: body === null ? undefined : JSON.stringify(body)
        });
    } catch (error) {
        throw new ApiError('网络请求失败，请确认后端服务已启动', 0, null);
    }

    const text = await response.text();
    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (error) {
            payload = null;
        }
    }

    if (!response.ok) {
        const message = payload?.message || `请求失败（${response.status}）`;
        if (response.status === 401 && auth) {
            clearSession();
            if (!location.pathname.endsWith('login.html')) {
                location.replace('login.html');
            }
        }
        throw new ApiError(message, response.status, payload?.data ?? null);
    }

    return payload?.data ?? null;
}

export const apiGet = (path, params) => api(path, { params });
export const apiPost = (path, body) => api(path, { method: 'POST', body });
export const apiPut = (path, body) => api(path, { method: 'PUT', body });
export const apiDelete = (path) => api(path, { method: 'DELETE' });
