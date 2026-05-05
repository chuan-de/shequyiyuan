export type FileUploadPayload = {
  file: File;
  businessType: string;
  businessId?: string;
  deduplicate?: boolean;
};

export async function uploadFile(payload: FileUploadPayload) {
  const formData = new FormData();
  formData.append("file", payload.file);
  formData.append("businessType", payload.businessType);
  if (payload.businessId) formData.append("businessId", payload.businessId);
  if (payload.deduplicate) formData.append("deduplicate", "true");

  const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/files`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${localStorage.getItem("access_token") ?? ""}`,
    },
    body: formData,
  });

  if (!response.ok) throw new Error("文件上传失败，请检查类型、大小或权限");
  return response.json();
}
