import { applyAuthResponse } from './api';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8089';

export type PhotoUploadResult = {
  id: string;
  url: string;
  contentType: string;
  sizeBytes: number;
  originalFilename: string | null;
};

export async function uploadPhoto(token: string, file: File): Promise<PhotoUploadResult> {
  const formData = new FormData();
  formData.append('file', file);

  const url = `${API_BASE_URL}/api/v1/photos`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });
  applyAuthResponse(response, url);

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('登录已过期，请重新登录后再上传');
    }
    let message = '图片上传失败';
    try {
      const body = await response.json();
      message = body?.message || message;
    } catch {
      // ignore JSON parse errors
    }
    throw new Error(message);
  }

  const wrapped = await response.json();
  return wrapped.data as PhotoUploadResult;
}
