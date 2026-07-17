package saas.com.br.resume_ai_saas.regeneration.service;

/**
 * Escapes user-controlled content before it is interpolated into the LaTeX
 * template. Prevents both compilation breakage and LaTeX command injection.
 */
public final class LatexEscaper {

    private LatexEscaper() {}

    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\textbackslash{}");
                case '{' -> sb.append("\\{");
                case '}' -> sb.append("\\}");
                case '$' -> sb.append("\\$");
                case '&' -> sb.append("\\&");
                case '#' -> sb.append("\\#");
                case '_' -> sb.append("\\_");
                case '%' -> sb.append("\\%");
                case '~' -> sb.append("\\textasciitilde{}");
                case '^' -> sb.append("\\textasciicircum{}");
                // Computer Modern lacks these glyphs — the compiler drops them
                // silently, so translate to the native LaTeX dash ligatures.
                case '—' -> sb.append("---");
                case '–' -> sb.append("--");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
