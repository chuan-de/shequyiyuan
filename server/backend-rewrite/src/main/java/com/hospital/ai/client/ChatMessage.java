package com.hospital.ai.client;

import java.util.List;
import java.util.Objects;

/**
 * OpenAI-compatible chat message.
 *
 * <p>Content can be either a plain string ({@link #text(String, String)}) or a
 * list of multimodal parts ({@link #multimodal(String, List)}) — the latter is
 * what vision calls use to attach an image alongside instructions.</p>
 */
public final class ChatMessage {

    private final String role;
    private final Object content;

    private ChatMessage(String role, Object content) {
        this.role = Objects.requireNonNull(role);
        this.content = Objects.requireNonNull(content);
    }

    public static ChatMessage text(String role, String content) {
        return new ChatMessage(role, content);
    }

    public static ChatMessage multimodal(String role, List<ContentPart> parts) {
        return new ChatMessage(role, List.copyOf(parts));
    }

    public String getRole() { return role; }
    public Object getContent() { return content; }

    /** Plain-text excerpt used by audit / logging. */
    public String contentAsText() {
        if (content instanceof String s) return s;
        if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object p : parts) {
                if (p instanceof ContentPart cp && cp.getText() != null) {
                    sb.append(cp.getText()).append(' ');
                }
            }
            return sb.toString().trim();
        }
        return String.valueOf(content);
    }

    /** Multimodal content part — either a text block or an image URL. */
    public static final class ContentPart {
        private final String type;
        private final String text;
        private final ImageUrl imageUrl;

        private ContentPart(String type, String text, ImageUrl imageUrl) {
            this.type = type;
            this.text = text;
            this.imageUrl = imageUrl;
        }

        public static ContentPart text(String text) {
            return new ContentPart("text", text, null);
        }

        public static ContentPart imageUrl(String url) {
            return new ContentPart("image_url", null, new ImageUrl(url));
        }

        public String getType() { return type; }
        public String getText() { return text; }
        public ImageUrl getImageUrl() { return imageUrl; }
    }

    public static final class ImageUrl {
        private final String url;

        public ImageUrl(String url) { this.url = url; }

        public String getUrl() { return url; }
    }
}
