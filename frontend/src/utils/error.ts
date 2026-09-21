import axios from 'axios';

interface ApiErrorBody {
  message?: string;
}

export function getErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback;
  return error.response?.data?.message ?? fallback;
}
