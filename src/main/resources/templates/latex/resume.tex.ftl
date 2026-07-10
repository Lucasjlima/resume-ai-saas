<#ftl output_format="plainText">
<#if compact>\documentclass[9pt,a4paper]{extarticle}<#else>\documentclass[11pt,a4paper]{article}</#if>
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage[margin=<#if compact>1.2cm<#else>2.2cm</#if>]{geometry}
\usepackage{enumitem}
\usepackage{titlesec}
\usepackage{parskip}
\pagestyle{empty}
<#if compact>\setlength{\parskip}{3pt}</#if>
\titleformat{\section}{<#if compact>\normalsize<#else>\large</#if>\bfseries}{}{0pt}{}[\titlerule]
\titlespacing*{\section}{0pt}{<#if compact>4pt<#else>10pt</#if>}{<#if compact>2pt<#else>6pt</#if>}
\setlist[itemize]{leftmargin=*,nosep,topsep=<#if compact>0pt<#else>2pt</#if>}

\begin{document}

\begin{center}
    {<#if compact>\Large<#else>\LARGE</#if> \textbf{${nome}}}\\[<#if compact>3pt<#else>6pt</#if>]
    <#if compact>{\small ${contato}}<#else>${contato}</#if>
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
<#if exp?has_next>\vspace{<#if compact>2pt<#else>6pt</#if>}</#if>
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
