import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useFormik } from 'formik';
import * as yup from 'yup';
import {
    Container,
    Paper,
    TextField,
    Button,
    Typography,
    Box,
    Alert,
    Link,
} from '@mui/material';
import { authAPI } from '../services/api';
import { loginStart, loginSuccess, loginFailure } from '../store/slices/authSlice';

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

const validationSchema = yup.object({
    username: yup.string().required('Username is required'),
    email: yup
        .string()
        .email('Enter a valid email address')
        .required('Email is required'),
    password: yup
        .string()
        .min(8, 'Password must be at least 8 characters')
        .matches(
            PASSWORD_PATTERN,
            'Password must contain uppercase, digit, and special character'
        )
        .required('Password is required'),
    confirmPassword: yup
        .string()
        .oneOf([yup.ref('password')], 'Passwords must match')
        .required('Please confirm your password'),
});

const Register: React.FC = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const [apiError, setApiError] = useState<string | null>(null);

    const formik = useFormik({
        initialValues: {
            username: '',
            email: '',
            password: '',
            confirmPassword: '',
        },
        validationSchema,
        onSubmit: async (values) => {
            setApiError(null);
            dispatch(loginStart());
            try {
                const response = await authAPI.register(
                    values.username,
                    values.email,
                    values.password
                );
                dispatch(loginSuccess(response.data));
                navigate('/');
            } catch (error: any) {
                const message =
                    error.response?.data?.error ||
                    error.response?.data?.username ||
                    error.response?.data?.email ||
                    error.response?.data?.password ||
                    'Registration failed. Please try again.';
                dispatch(loginFailure(message));
                setApiError(message);
            }
        },
    });

    return (
        <Container component="main" maxWidth="xs">
            <Box
                sx={{
                    marginTop: 8,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
                    <Typography component="h1" variant="h5" align="center" gutterBottom>
                        Create Account
                    </Typography>

                    {apiError && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {apiError}
                        </Alert>
                    )}

                    <form onSubmit={formik.handleSubmit}>
                        <TextField
                            fullWidth
                            margin="normal"
                            id="username"
                            name="username"
                            label="Username"
                            value={formik.values.username}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.username && Boolean(formik.errors.username)}
                            helperText={formik.touched.username && formik.errors.username}
                        />
                        <TextField
                            fullWidth
                            margin="normal"
                            id="email"
                            name="email"
                            label="Email Address"
                            type="email"
                            value={formik.values.email}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.email && Boolean(formik.errors.email)}
                            helperText={formik.touched.email && formik.errors.email}
                        />
                        <TextField
                            fullWidth
                            margin="normal"
                            id="password"
                            name="password"
                            label="Password"
                            type="password"
                            value={formik.values.password}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.password && Boolean(formik.errors.password)}
                            helperText={formik.touched.password && formik.errors.password}
                        />
                        <TextField
                            fullWidth
                            margin="normal"
                            id="confirmPassword"
                            name="confirmPassword"
                            label="Confirm Password"
                            type="password"
                            value={formik.values.confirmPassword}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={
                                formik.touched.confirmPassword &&
                                Boolean(formik.errors.confirmPassword)
                            }
                            helperText={
                                formik.touched.confirmPassword && formik.errors.confirmPassword
                            }
                        />
                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            sx={{ mt: 3, mb: 2 }}
                            disabled={formik.isSubmitting}
                        >
                            Register
                        </Button>
                    </form>

                    <Box sx={{ textAlign: 'center', mt: 1 }}>
                        <Typography variant="body2">
                            Already have an account?{' '}
                            <Link component={RouterLink} to="/login">
                                Login
                            </Link>
                        </Typography>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};

export default Register;
