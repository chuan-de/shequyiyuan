"use client";

import { useState } from "react";
import { uploadFile } from "@/lib/file";

type Props = {
  businessType: string;
  businessId?: string;
  deduplicate?: boolean;
  onUploaded?: (result: unknown) => void;
};

export function FileUpload({ businessType, businessId, deduplicate, onUploaded }: Props) {
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState(false);

  const onChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setLoading(true);
    setError("");
    try {
      const result = await uploadFile({ file, businessType, businessId, deduplicate });
      onUploaded?.(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "上传失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-2">
      <input type="file" onChange={onChange} disabled={loading} className="block w-full text-sm" />
      {loading ? <p className="text-sm text-gray-500">上传中...</p> : null}
      {error ? <p className="text-sm text-red-600">{error}</p> : null}
    </div>
  );
}
