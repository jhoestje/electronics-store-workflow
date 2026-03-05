import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import Register from '../pages/Register';
import authReducer from '../store/slices/authSlice';
import cartReducer from '../store/slices/cartSlice';
import * as api from '../services/api';

vi.mock('../services/api', () => ({
    authAPI: {
        register: vi.fn(),
    },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

function renderRegister() {
    const store = configureStore({
        reducer: { auth: authReducer, cart: cartReducer },
    });
    return render(
        <Provider store={store}>
            <MemoryRouter>
                <Register />
            </MemoryRouter>
        </Provider>
    );
}

describe('Register page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders all form fields and register button', () => {
        renderRegister();

        expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
    });

    it('renders a link to the login page', () => {
        renderRegister();

        expect(screen.getByRole('link', { name: /login/i })).toBeInTheDocument();
    });

    it('shows validation errors when submitting empty form', async () => {
        renderRegister();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/username is required/i)).toBeInTheDocument();
            expect(screen.getByText(/email is required/i)).toBeInTheDocument();
            expect(screen.getByText(/password is required/i)).toBeInTheDocument();
        });
    });

    it('shows error when email format is invalid', async () => {
        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/email address/i), 'not-an-email');
        await user.tab();

        await waitFor(() => {
            expect(screen.getByText(/enter a valid email address/i)).toBeInTheDocument();
        });
    });

    it('shows error when password does not meet strength requirements', async () => {
        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/^password$/i), 'weakpass');
        await user.tab();

        await waitFor(() => {
            expect(
                screen.getByText(/password must contain uppercase, digit, and special character/i)
            ).toBeInTheDocument();
        });
    });

    it('shows error when passwords do not match', async () => {
        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/^password$/i), 'Secure1@pass');
        await user.type(screen.getByLabelText(/confirm password/i), 'DifferentPass1@');
        await user.tab();

        await waitFor(() => {
            expect(screen.getByText(/passwords must match/i)).toBeInTheDocument();
        });
    });

    it('calls authAPI.register and navigates home on successful registration', async () => {
        const mockRegister = vi.mocked(api.authAPI.register);
        mockRegister.mockResolvedValueOnce({
            data: {
                token: 'jwt-test-token',
                user: { id: 1, username: 'johndoe', email: 'john@example.com', roles: ['ROLE_CUSTOMER'] },
            },
        } as any);

        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/username/i), 'johndoe');
        await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'Secure1@pass');
        await user.type(screen.getByLabelText(/confirm password/i), 'Secure1@pass');
        await user.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(mockRegister).toHaveBeenCalledWith('johndoe', 'john@example.com', 'Secure1@pass');
            expect(mockNavigate).toHaveBeenCalledWith('/');
        });
    });

    it('shows API error alert when username already exists', async () => {
        const mockRegister = vi.mocked(api.authAPI.register);
        mockRegister.mockRejectedValueOnce({
            response: { data: { error: 'Username already exists' } },
        });

        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/username/i), 'johndoe');
        await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'Secure1@pass');
        await user.type(screen.getByLabelText(/confirm password/i), 'Secure1@pass');
        await user.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByRole('alert')).toHaveTextContent('Username already exists');
        });
    });

    it('shows API error alert when email already exists', async () => {
        const mockRegister = vi.mocked(api.authAPI.register);
        mockRegister.mockRejectedValueOnce({
            response: { data: { error: 'Email already exists' } },
        });

        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/username/i), 'johndoe');
        await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'Secure1@pass');
        await user.type(screen.getByLabelText(/confirm password/i), 'Secure1@pass');
        await user.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByRole('alert')).toHaveTextContent('Email already exists');
        });
    });

    it('shows fallback error message on unexpected API failure', async () => {
        const mockRegister = vi.mocked(api.authAPI.register);
        mockRegister.mockRejectedValueOnce(new Error('Network Error'));

        renderRegister();
        const user = userEvent.setup();

        await user.type(screen.getByLabelText(/username/i), 'johndoe');
        await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'Secure1@pass');
        await user.type(screen.getByLabelText(/confirm password/i), 'Secure1@pass');
        await user.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByRole('alert')).toHaveTextContent(
                'Registration failed. Please try again.'
            );
        });
    });
});
