package saas.com.br.resume_ai_saas.regeneration.service;

/**
 * Layout density ladder for the one-page guarantee: the pipeline compiles with
 * the least dense layout that fits a single page, so short resumes keep the
 * airy layout and long ones are progressively compressed. Section spacing is
 * declared with stretchable glue ("plus") so, combined with {@code \flushbottom},
 * the last section ends at the bottom of the page instead of leaving a white gap.
 */
public enum LayoutDensity {

    // sectionBefore carries "plus 1fil": combined with \flushbottom, leftover
    // vertical space is distributed between sections so the last one ends near
    // the bottom of the page instead of leaving a single white block.
    NORMAL("\\documentclass[11pt,a4paper]{article}", "2.2cm", "6pt plus 2pt",
            "\\large", "10pt plus 1fil minus 2pt", "6pt", "2pt",
            "\\LARGE", "6pt", false, "6pt plus 4pt"),

    MEDIUM("\\documentclass[10pt,a4paper]{extarticle}", "1.6cm", "5pt plus 2pt",
            "\\large", "7pt plus 1fil minus 2pt", "4pt", "1pt",
            "\\LARGE", "5pt", false, "4pt plus 3pt"),

    COMPACT("\\documentclass[9pt,a4paper]{extarticle}", "1.2cm", "3pt plus 1pt",
            "\\normalsize", "4pt plus 1fil minus 1pt", "2pt", "0pt",
            "\\Large", "3pt", true, "2pt plus 2pt");

    private final String documentClass;
    private final String margin;
    private final String parskip;
    private final String sectionFont;
    private final String sectionBefore;
    private final String sectionAfter;
    private final String itemTopsep;
    private final String nameSize;
    private final String nameGap;
    private final boolean smallContact;
    private final String experienceGap;

    LayoutDensity(String documentClass, String margin, String parskip,
                  String sectionFont, String sectionBefore, String sectionAfter,
                  String itemTopsep, String nameSize, String nameGap,
                  boolean smallContact, String experienceGap) {
        this.documentClass = documentClass;
        this.margin = margin;
        this.parskip = parskip;
        this.sectionFont = sectionFont;
        this.sectionBefore = sectionBefore;
        this.sectionAfter = sectionAfter;
        this.itemTopsep = itemTopsep;
        this.nameSize = nameSize;
        this.nameGap = nameGap;
        this.smallContact = smallContact;
        this.experienceGap = experienceGap;
    }

    public String documentClass() { return documentClass; }
    public String margin() { return margin; }
    public String parskip() { return parskip; }
    public String sectionFont() { return sectionFont; }
    public String sectionBefore() { return sectionBefore; }
    public String sectionAfter() { return sectionAfter; }
    public String itemTopsep() { return itemTopsep; }
    public String nameSize() { return nameSize; }
    public String nameGap() { return nameGap; }
    public boolean smallContact() { return smallContact; }
    public String experienceGap() { return experienceGap; }
}
