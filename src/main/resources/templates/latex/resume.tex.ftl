<#ftl output_format="plainText">
\documentclass[11pt,a4paper]{article}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage[margin=2.2cm]{geometry}
\usepackage{enumitem}
\usepackage{titlesec}
\usepackage{parskip}
\pagestyle{empty}
\titleformat{\section}{\large\bfseries}{}{0pt}{}[\titlerule]
\titlespacing*{\section}{0pt}{10pt}{6pt}
\setlist[itemize]{leftmargin=*,nosep,topsep=2pt}

\begin{document}

\begin{center}
    {\LARGE \textbf{${nome}}}\\[6pt]
    ${contato}
\end{center}

\section{Resumo Profissional}
${resumo}

<#if experiencias?size gt 0>
\section{Experiência Profissional}
<#list experiencias as exp>
\textbf{${exp.cargo}} --- ${exp.empresa} \hfill ${exp.periodo}
<#if exp.bullets?size gt 0>
\begin{itemize}
<#list exp.bullets as bullet>
    \item ${bullet}
</#list>
\end{itemize}
</#if>
<#if exp?has_next>\vspace{6pt}</#if>
</#list>
</#if>

<#if educacao?size gt 0>
\section{Formação Acadêmica}
<#list educacao as edu>
\textbf{${edu.curso}} --- ${edu.instituicao} \hfill ${edu.periodo}\par
</#list>
</#if>

<#if skills?size gt 0>
\section{Habilidades}
${skills?join(" \\textbullet{} ")}
</#if>

<#if certificacoes?size gt 0>
\section{Certificações}
<#list certificacoes as cert>
\textbf{${cert.nome}}<#if cert.instituicao != ""> --- ${cert.instituicao}</#if><#if cert.data != ""> \hfill ${cert.data}</#if>\par
</#list>
</#if>

<#if idiomas?size gt 0>
\section{Idiomas}
<#list idiomas as idioma>
\textbf{${idioma.idioma}}<#if idioma.nivel != ""> --- ${idioma.nivel}</#if><#if idioma?has_next> \textbullet{} </#if>
</#list>
</#if>

\end{document}
