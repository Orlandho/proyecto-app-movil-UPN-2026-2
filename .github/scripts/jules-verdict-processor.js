/**
 * jules-verdict-processor.js
 * 
 * Script procesador del veredicto emitido por el Agente Jules en su Sandbox Virtual.
 * Se ejecuta ante el evento 'issue_comment' en GitHub Actions.
 * 
 * Acciones:
 * 1. Extrae los metadatos estructurados del Issue (PR_NUMBER, COMMIT_SHA, BRANCH).
 * 2. Analiza el contenido del comentario en busca del veredicto de Jules:
 *    - VEREDICTO: APROBADO -> Marca Status Check como SUCCESS y notifica al PR.
 *    - VEREDICTO: RECHAZADO -> Marca Status Check como FAILURE y notifica al PR con los motivos.
 *    - Mensajes intermedios -> Registra el avance sin alterar el estado PENDING.
 * 3. Registra logs detallados para trazabilidad completa.
 */

const fs = require('fs');

const GITHUB_TOKEN = process.env.GITHUB_TOKEN || process.env.GITHUB_PERSONAL_ACCESS_TOKEN;
const GITHUB_REPOSITORY = process.env.GITHUB_REPOSITORY || 'Orlandho/proyecto-app-movil-UPN-2026-2';
const GITHUB_API_URL = process.env.GITHUB_API_URL || 'https://api.github.com';

const STATUS_CONTEXT = 'Veredicto de Auditoría en Sandbox de Jules';

if (!GITHUB_TOKEN) {
  console.error('❌ ERROR CRÍTICO: GITHUB_TOKEN no está definido en el entorno.');
  process.exit(1);
}

// Cliente HTTP para GitHub API con reintentos
async function ghRequest(endpoint, method = 'GET', body = null, retries = 3) {
  const url = `${GITHUB_API_URL}${endpoint}`;
  const headers = {
    'Authorization': `Bearer ${GITHUB_TOKEN}`,
    'Accept': 'application/vnd.github+json',
    'User-Agent': 'Jules-Verdict-Processor/1.0',
    'X-GitHub-Api-Version': '2022-11-28',
    'Content-Type': 'application/json'
  };

  const options = { method, headers };
  if (body) {
    options.body = JSON.stringify(body);
  }

  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const res = await fetch(url, options);
      const responseData = await res.json().catch(() => null);

      if (!res.ok) {
        const errorMsg = responseData?.message || `HTTP ${res.status} ${res.statusText}`;
        throw new Error(`GitHub API [${method} ${endpoint}] falló: ${errorMsg}`);
      }

      return responseData;
    } catch (err) {
      if (attempt === retries) {
        throw err;
      }
      console.warn(`⚠️ Error en petición HTTP (${err.message}). Reintentando (${attempt}/${retries})...`);
      await new Promise(resolve => setTimeout(resolve, 2000 * attempt));
    }
  }
}

// Actualiza el status check del commit
async function setCommitStatus(sha, state, description, targetUrl = null) {
  console.log(`📡 Actualizando Status Check [${STATUS_CONTEXT}] -> ${state.toUpperCase()}: "${description}"`);
  const payload = {
    state,
    description: description.substring(0, 140),
    context: STATUS_CONTEXT
  };
  if (targetUrl) {
    payload.target_url = targetUrl;
  }
  return await ghRequest(`/repos/${GITHUB_REPOSITORY}/statuses/${sha}`, 'POST', payload);
}

// Publica un comentario en un Issue o PR
async function postComment(issueOrPrNumber, body) {
  return await ghRequest(`/repos/${GITHUB_REPOSITORY}/issues/${issueOrPrNumber}/comments`, 'POST', { body });
}

// Extrae metadatos del comentario oculto en el Issue
function parseMetadata(issueBody) {
  if (!issueBody) return null;
  const match = issueBody.match(/<!--\s*JULES_AUDIT_METADATA\s*([\s\S]*?)-->/);
  if (!match) return null;

  const metadata = {};
  const lines = match[1].split('\n');
  for (const line = lines) {
    const parts = line.split(':');
    if (parts.length >= 2) {
      const key = parts[0].trim();
      const val = parts.slice(1).join(':').trim();
      metadata[key] = val;
    }
  }
  return metadata;
}

async function main() {
  console.log('================================================================');
  console.log('🤖 PROCESANDO COMENTARIO DEL AGENTE JULES PARA VEREDICTO DE AUDITORÍA');
  console.log('================================================================');

  const eventPath = process.env.GITHUB_EVENT_PATH;
  if (!eventPath || !fs.existsSync(eventPath)) {
    console.error('❌ No se encontró GITHUB_EVENT_PATH.');
    process.exit(1);
  }

  const eventData = JSON.parse(fs.readFileSync(eventPath, 'utf8'));
  const issue = eventData.issue;
  const comment = eventData.comment;

  if (!issue || !comment) {
    console.log('ℹ️ El evento no contiene issue o comment. Finalizando.');
    process.exit(0);
  }

  // 1. Verificar si el Issue es de auditoría de Jules
  const metadata = parseMetadata(issue.body);
  if (!metadata || !metadata.PR_NUMBER || !metadata.COMMIT_SHA) {
    console.log('ℹ️ El Issue no contiene metadatos de auditoría de Jules. Ignorando.');
    process.exit(0);
  }

  const prNumber = metadata.PR_NUMBER;
  const commitSha = metadata.COMMIT_SHA;
  const branch = metadata.BRANCH;
  const commentAuthor = comment.user?.login || 'desconocido';
  const commentBody = comment.body || '';
  const commentUrl = comment.html_url || issue.html_url;

  console.log(`📋 Metadatos identificados:`);
  console.log(`   - PR: #${prNumber}`);
  console.log(`   - Commit SHA: ${commitSha}`);
  console.log(`   - Rama: ${branch}`);
  console.log(`   - Autor del comentario: @${commentAuthor}`);
  console.log(`   - URL del comentario: ${commentUrl}`);

  // 2. Analizar el contenido del comentario en busca del veredicto
  const isApproved = /(?:VEREDICTO|VERDICT):\s*(?:APROBADO|APPROVED)/i.test(commentBody);
  const isRejected = /(?:VEREDICTO|VERDICT):\s*(?:RECHAZADO|REJECTED|FALLIDO|FAILED)/i.test(commentBody);

  if (isApproved) {
    console.log('🎉 [VEREDICTO DETECTADO]: APROBADO por el Agente Jules.');

    // Marcar Status Check en SUCCESS
    await setCommitStatus(
      commitSha,
      'success',
      '✅ Aprobado por el Agente Jules en entorno virtual',
      commentUrl
    );

    // Publicar comentario de aprobación en el PR
    const prApprovalComment = `### 🟢 [Agente Jules] Auditoría Virtual APROBADA

El Agente Jules ha completado la auditoría de este Pull Request en su entorno virtual con resultado favorable:
- **Veredicto:** \`VEREDICTO: APROBADO\`
- **Status Check:** \`${STATUS_CONTEXT}\` -> **SUCCESS (🟢 Aprobado)**
- **Detalle de la Evaluación:** [Ver análisis completo en el Issue #${issue.number}](${commentUrl})

> ✨ *La validación en sandbox concluyó con éxito. Este requerimiento de protección ha sido desbloqueado.*
`;
    await postComment(prNumber, prApprovalComment);
    console.log(`✅ Status Check actualizado a SUCCESS y notificación enviada al PR #${prNumber}.`);

  } else if (isRejected) {
    console.log('🛑 [VEREDICTO DETECTADO]: RECHAZADO por el Agente Jules.');

    // Marcar Status Check en FAILURE
    await setCommitStatus(
      commitSha,
      'failure',
      '❌ Rechazado por el Agente Jules. Se detectaron problemas en el código.',
      commentUrl
    );

    // Publicar comentario de rechazo en el PR
    const prRejectionComment = `### 🔴 [Agente Jules] Auditoría Virtual RECHAZADA

El Agente Jules ha analizado la rama \`${branch}\` en su sandbox virtual y **ha detectado inconsistencias o fallos técnicos**:
- **Veredicto:** \`VEREDICTO: RECHAZADO\`
- **Status Check:** \`${STATUS_CONTEXT}\` -> **FAILURE (🔴 Bloqueado)**
- **Reporte Completo de Errores:** [Ver hallazgos detallados en el Issue #${issue.number}](${commentUrl})

---

#### 📌 Acciones Requeridas:
1. Revisa las observaciones y recomendaciones formuladas por Jules en el [comentario de auditoría](${commentUrl}).
2. Corrige las aserciones, reglas de \`AGENTS.md\` o defectos señalados en tu rama local.
3. Haz un nuevo \`git push\` a la rama del PR o agrega la etiqueta \`reintentar-jules\` tras aplicar las correcciones para relanzar la auditoría en la nube.
`;
    await postComment(prNumber, prRejectionComment);
    console.log(`❌ Status Check actualizado a FAILURE y alerta de remediación enviada al PR #${prNumber}.`);

  } else {
    console.log('ℹ️ Comentario recibido sin veredicto conclusivo (posible mensaje intermedio de avance de Jules).');
    console.log('Se mantiene el Status Check en su estado actual (PENDING).');
  }

  process.exit(0);
}

main().catch(err => {
  console.error(`💥 Error no controlado en el procesador de veredictos: ${err.stack || err.message}`);
  process.exit(1);
});
