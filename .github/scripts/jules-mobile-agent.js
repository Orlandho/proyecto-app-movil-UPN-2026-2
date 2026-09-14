#!/usr/bin/env node

/**
 * 🤖 Agente Jules - Orquestador Inteligente de Pruebas Móviles (Android / Kotlin)
 * Repositorio: proyecto app movil UPN 2026-2 (FoodJet Móvil)
 *
 * Módulos adaptados para aplicaciones móviles:
 * 1. static       - Pruebas Estáticas (Compilación Kotlin, seguridad OWASP Mobile Top 10, permisos Android).
 * 2. unit         - Pruebas de Caja Blanca y Unitarias (Modelos de Dominio, DTOs, ViewModels con JUnit real).
 * 3. functional   - Pruebas Funcionales y Caja Negra (Contratos REST Retrofit, DTO mapping, flujos de usuario).
 * 4. regression   - Pruebas de Regresión (Estabilidad de modelos, compatibilidad hacia atrás y blast radius).
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Configuración y variables de entorno
const TEST_TYPE = process.env.TEST_TYPE || process.argv[2] || 'static';
const JULES_API_KEY = process.env.JULES_API_KEY || '';
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || '';
const GITHUB_REPOSITORY = process.env.GITHUB_REPOSITORY || '';
const PR_NUMBER = process.env.PR_NUMBER || '';
const COMMIT_SHA = process.env.COMMIT_SHA || process.env.GITHUB_SHA || '';
const SUMMARY_FILE = process.env.GITHUB_STEP_SUMMARY || '';
const WORKSPACE_DIR = process.cwd();
const TEST_RESULTS_DIR = path.join(WORKSPACE_DIR, 'app', 'build', 'test-results', 'testDebugUnitTest');

// --- 1. Detección y delimitación de cambios vía Git Diff (Alcance Móvil) ---
function getGitDiffScope() {
    let changedFiles = [];
    let patchContent = '';

    const changedFilesPath = path.join(WORKSPACE_DIR, 'changed_files.txt');
    const patchPath = path.join(WORKSPACE_DIR, 'diff.patch');

    if (fs.existsSync(changedFilesPath)) {
        changedFiles = fs.readFileSync(changedFilesPath, 'utf8')
            .split('\n')
            .map(f => f.trim())
            .filter(Boolean);
    } else {
        try {
            const out = execSync('git diff --name-only HEAD~1 HEAD 2>nul || git diff --name-only HEAD', { encoding: 'utf8' });
            changedFiles = out.split('\n').map(f => f.trim()).filter(Boolean);
        } catch {
            changedFiles = [];
        }
    }

    if (fs.existsSync(patchPath)) {
        patchContent = fs.readFileSync(patchPath, 'utf8');
    } else {
        try {
            patchContent = execSync('git diff HEAD~1 HEAD 2>nul || git diff HEAD', { encoding: 'utf8' });
        } catch {
            patchContent = '';
        }
    }

    return { changedFiles, patchContent };
}

// --- 2. Parser de Resultados XML de JUnit emitidos por Gradle ---
function parseJUnitResults(testDir) {
    const summary = {
        totalTests: 0,
        passed: 0,
        failures: 0,
        errors: 0,
        skipped: 0,
        timeSeconds: 0,
        testCases: [],
        failedCases: []
    };

    if (!fs.existsSync(testDir)) {
        return summary;
    }

    const files = fs.readdirSync(testDir).filter(f => f.startsWith('TEST-') && f.endsWith('.xml'));
    for (const file of files) {
        const filePath = path.join(testDir, file);
        const content = fs.readFileSync(filePath, 'utf8');

        // Extraer estadísticas del suite
        const suiteMatch = content.match(/<testsuite[^>]*tests="(\d+)"[^>]*skipped="(\d+)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"[^>]*time="([^"]+)"/);
        if (suiteMatch) {
            summary.totalTests += parseInt(suiteMatch[1], 10);
            summary.skipped += parseInt(suiteMatch[2], 10);
            summary.failures += parseInt(suiteMatch[3], 10);
            summary.errors += parseInt(suiteMatch[4], 10);
            summary.timeSeconds += parseFloat(suiteMatch[5]) || 0;
        }

        // Extraer casos individuales
        const caseRegex = /<testcase\s+name="([^"]+)"\s+classname="([^"]+)"\s+time="([^"]+)"(?:\s*\/>|>([\s\S]*?)<\/testcase>)/g;
        let match;
        while ((match = caseRegex.exec(content)) !== null) {
            const name = match[1];
            const className = match[2];
            const time = match[3];
            const inner = match[4] || '';

            const isFailure = inner.includes('<failure');
            const isError = inner.includes('<error');

            let failureMessage = '';
            if (isFailure) {
                const failMatch = inner.match(/<failure\s+message="([^"]*)"/);
                failureMessage = failMatch ? failMatch[1] : (inner.match(/<failure[^>]*>([\s\S]*?)<\/failure>/)?.[1] || 'Assertion failed');
            } else if (isError) {
                const errMatch = inner.match(/<error\s+message="([^"]*)"/);
                failureMessage = errMatch ? errMatch[1] : (inner.match(/<error[^>]*>([\s\S]*?)<\/error>/)?.[1] || 'Execution error');
            }

            const tc = {
                name,
                className,
                time: parseFloat(time) || 0,
                status: (isFailure || isError) ? 'FALLIDO' : 'EXITOSO',
                failureMessage: failureMessage.replace(/&quot;/g, '"').replace(/&lt;/g, '<').replace(/&gt;/g, '>').trim().substring(0, 300)
            };
            summary.testCases.push(tc);
            if (isFailure || isError) {
                summary.failedCases.push(tc);
            }
        }
    }

    summary.passed = Math.max(0, summary.totalTests - summary.failures - summary.errors - summary.skipped);
    return summary;
}

function clearTestResults(testDir) {
    if (fs.existsSync(testDir)) {
        const files = fs.readdirSync(testDir).filter(f => f.startsWith('TEST-') && f.endsWith('.xml'));
        for (const file of files) {
            try { fs.unlinkSync(path.join(testDir, file)); } catch {}
        }
    }
}

// --- 3. Invocación al Agente Jules (API o Evaluador Local Resiliente) ---
async function invokeJulesAgent(taskDescription, contextData) {
    if (!JULES_API_KEY) {
        return null;
    }

    const prompt = `Actúa como el agente de aseguramiento de calidad y automatización móvil Jules.
Tu objetivo es analizar los cambios de código y validar los resultados de pruebas del repositorio Android Kotlin "FoodJet Móvil" (proyecto app movil UPN 2026-2).

TAREA ESPECÍFICA:
${taskDescription}

CONTEXTO TÉCNICO Y RESULTADOS REALES DE EJECUCIÓN:
${JSON.stringify(contextData, null, 2)}

Por favor devuelve tu dictamen en formato Markdown con las siguientes secciones:
1. Resumen Ejecutivo y Veredicto Móvil (Aprobado / Advertencias / Requiere Correcciones)
2. Hallazgos Específicos de Pruebas
3. Recomendaciones Claras para Arquitectura Android (MVVM / Jetpack Compose)`;

    try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(JULES_API_KEY)}`;
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: { temperature: 0.2, maxOutputTokens: 2048 }
            }),
            signal: AbortSignal.timeout(5000)
        });

        if (response.ok) {
            const json = await response.json();
            const text = json.candidates?.[0]?.content?.parts?.[0]?.text;
            if (text) {
                console.log('✅ Dictamen recibido directamente de la API del Agente Jules.');
                return text;
            }
        }
    } catch (err) {
        // Fallback silencioso y limpio
    }

    return null;
}

// --- 4. Ejecutor 1: Pruebas Estáticas para App Móvil (Compilación Kotlin + OWASP) ---
async function runMobileStaticAnalysis(scope) {
    console.log('\n--- [1] EJECUTANDO ANÁLISIS ESTÁTICO Y COMPILACIÓN KOTLIN CON AGENTE JULES ---');
    const { changedFiles, patchContent } = scope;
    const findings = [];
    const gradlewCmd = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew';

    // 1. Verificación de Compilación Real de Fuentes Kotlin
    let compileSuccess = true;
    let compileOutput = '';
    try {
        console.log('Ejecutando compilación estática de Kotlin: compileDebugKotlin compileDebugUnitTestKotlin...');
        compileOutput = execSync(`${gradlewCmd} compileDebugKotlin compileDebugUnitTestKotlin --no-daemon`, {
            encoding: 'utf8',
            cwd: WORKSPACE_DIR,
            timeout: 180000
        });
        console.log('✅ Compilación estática de Kotlin finalizada exitosamente.');
    } catch (err) {
        compileSuccess = false;
        compileOutput = (err.stdout || '') + '\n' + (err.stderr || '');
        console.error('❌ Error de compilación estática en fuentes Kotlin.');
        
        // Extraer líneas de error del compilador
        const errorLines = compileOutput.split('\n').filter(l => l.includes('e: ') || l.includes('Compilation error'));
        errorLines.slice(0, 5).forEach(el => {
            findings.push({
                severity: 'CRÍTICA',
                file: 'Kotlin Compiler',
                line: '-',
                category: 'Error de Compilación Estática Kotlin',
                description: el.trim()
            });
        });

        if (findings.length === 0) {
            findings.push({
                severity: 'CRÍTICA',
                file: 'Kotlin Compiler',
                line: '-',
                category: 'Error de Compilación Estática Kotlin',
                description: 'La compilación estática de Kotlin falló con errores en las fuentes.'
            });
        }
    }

    // 2. Reglas de Seguridad OWASP Mobile Top 10 y Calidad Android
    const mobileFiles = changedFiles.filter(f =>
        /\.(kt|kts|xml|json|toml|properties)$/.test(f) &&
        !f.includes('.idea') &&
        !f.includes('.gradle') &&
        !f.includes('build/') &&
        fs.existsSync(path.join(WORKSPACE_DIR, f))
    );

    const secretRegex = /(api[_-]?key|password|secret|jwt_secret|private_key|token)\s*[:=]\s*['"][a-zA-Z0-9_\-\.]{14,}['"]/i;
    const cleartextTrafficRegex = /android:usesCleartextTraffic\s*=\s*["']true["']/;
    const insecureLoggingRegex = /Log\.(d|v|i)\(.*?(password|token|tarjeta|secret|clave|pin).*?\)/i;
    const blockingThreadRegex = /Thread\.sleep\s*\(/;
    const globalScopeRegex = /GlobalScope\.(launch|async)/;

    for (const relFile of mobileFiles) {
        const absPath = path.join(WORKSPACE_DIR, relFile);
        const content = fs.readFileSync(absPath, 'utf8');
        const lines = content.split('\n');

        if (relFile.endsWith('AndroidManifest.xml') && cleartextTrafficRegex.test(content)) {
            findings.push({
                severity: 'ALTA',
                file: relFile,
                category: 'Seguridad Móvil (OWASP M4 / Tráfico No Seguro)',
                description: 'android:usesCleartextTraffic="true" permite tráfico HTTP en texto claro. Se recomienda HTTPS estricto.'
            });
        }

        lines.forEach((line, idx) => {
            const lineNum = idx + 1;
            const trimmed = line.trim();
            if (trimmed.startsWith('//') || trimmed.startsWith('/*') || trimmed.startsWith('*')) return;

            if (secretRegex.test(line) && !relFile.includes('test') && !relFile.includes('example')) {
                findings.push({
                    severity: 'CRÍTICA',
                    file: relFile,
                    line: lineNum,
                    category: 'Seguridad Móvil (OWASP M1 / Credenciales Expuestas)',
                    description: 'Posible secreto o clave privada expuesta directamente en el código móvil.'
                });
            }

            if (insecureLoggingRegex.test(line)) {
                findings.push({
                    severity: 'ALTA',
                    file: relFile,
                    line: lineNum,
                    category: 'Seguridad Móvil (OWASP M3 / Logging Inseguro)',
                    description: 'Registro de datos sensibles en Logcat.'
                });
            }

            if (blockingThreadRegex.test(line) && !relFile.includes('test')) {
                findings.push({
                    severity: 'MEDIA',
                    file: relFile,
                    line: lineNum,
                    category: 'Rendimiento y ANR',
                    description: 'Uso de Thread.sleep() en hilo de ejecución.'
                });
            }

            if (globalScopeRegex.test(line)) {
                findings.push({
                    severity: 'MEDIA',
                    file: relFile,
                    line: lineNum,
                    category: 'Arquitectura Kotlin (Corrutinas)',
                    description: 'Uso de GlobalScope desaconsejado. Usar viewModelScope.'
                });
            }
        });
    }

    const hasCritical = findings.some(f => f.severity === 'CRÍTICA' || f.severity === 'ALTA') || !compileSuccess;
    const status = hasCritical ? 'FALLIDO' : (findings.length > 0 ? 'ADVERTENCIA' : 'EXITOSO');

    const julesFeedback = await invokeJulesAgent(
        'Analiza las directrices de calidad, compilabilidad Kotlin y seguridad OWASP Mobile.',
        { totalFiles: mobileFiles.length, compileSuccess, findingsCount: findings.length, findings }
    );

    return {
        name: 'Análisis Estático de App Móvil con Jules',
        status,
        compileSuccess,
        filesAnalyzed: mobileFiles.length,
        findings,
        julesFeedback,
        metrics: {
            archivosAnalizados: mobileFiles.length,
            compilacionExitosa: compileSuccess ? '✅ Compilación Kotlin Conforme' : '❌ Fallo en Compilación Kotlin',
            hallazgosCriticos: findings.filter(f => f.severity === 'CRÍTICA').length,
            hallazgosAltos: findings.filter(f => f.severity === 'ALTA').length,
            hallazgosMedios: findings.filter(f => f.severity === 'MEDIA').length,
            hallazgosBajos: findings.filter(f => f.severity === 'BAJA').length
        }
    };
}

// --- 5. Ejecutor 2: Pruebas de Caja Blanca y Unitarias para App Móvil ---
async function runMobileUnitWhiteboxAnalysis(scope) {
    console.log('\n--- [2] EJECUTANDO PRUEBAS DE CAJA BLANCA Y UNITARIAS REALES CON GRADLE ---');
    const gradlewCmd = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew';
    clearTestResults(TEST_RESULTS_DIR);

    let gradleSuccess = true;
    let rawOutput = '';
    try {
        console.log('Ejecutando suite unitaria: FoodJetDomainUnitTest + ExampleUnitTest...');
        rawOutput = execSync(`${gradlewCmd} testDebugUnitTest --tests "com.example.foodjeetapp.FoodJetDomainUnitTest" --tests "com.example.foodjeetapp.ExampleUnitTest" --tests "com.example.foodjeetapp.unit.*" --tests "com.example.foodjeetapp.audit.*" --no-daemon --stacktrace`, {
            encoding: 'utf8',
            cwd: WORKSPACE_DIR,
            timeout: 180000
        });
        console.log('✅ Gradle suite unitaria finalizado.');
    } catch (err) {
        gradleSuccess = false;
        rawOutput = (err.stdout || '') + '\n' + (err.stderr || '');
        console.error('❌ Fallo detectado en la suite de pruebas unitarias.');
    }

    const testSummary = parseJUnitResults(TEST_RESULTS_DIR);
    const testSuccess = gradleSuccess && testSummary.failures === 0 && testSummary.errors === 0 && testSummary.totalTests > 0;
    const status = testSuccess ? 'EXITOSO' : 'FALLIDO';

    const julesFeedback = await invokeJulesAgent(
        'Inspecciona las pruebas unitarias y de caja blanca en modelos de dominio y DTOs.',
        { testSuccess, testSummary }
    );

    return {
        name: 'Pruebas Unitarias y Cobertura de Ramas Móvil con Jules',
        status,
        testSuccess,
        testSummary,
        julesFeedback
    };
}

// --- 6. Ejecutor 3: Pruebas Funcionales y de Caja Negra para App Móvil ---
async function runMobileFunctionalBlackboxAnalysis(scope) {
    console.log('\n--- [3] EJECUTANDO PRUEBAS FUNCIONALES Y DE CAJA NEGRA REALES CON GRADLE ---');
    const gradlewCmd = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew';
    clearTestResults(TEST_RESULTS_DIR);

    let gradleSuccess = true;
    let rawOutput = '';
    try {
        console.log('Ejecutando suite funcional: FoodJetFunctionalBlackboxTest...');
        rawOutput = execSync(`${gradlewCmd} testDebugUnitTest --tests "com.example.foodjeetapp.functional.*" --no-daemon --stacktrace`, {
            encoding: 'utf8',
            cwd: WORKSPACE_DIR,
            timeout: 180000
        });
        console.log('✅ Gradle suite funcional finalizado.');
    } catch (err) {
        gradleSuccess = false;
        rawOutput = (err.stdout || '') + '\n' + (err.stderr || '');
        console.error('❌ Fallo detectado en la suite de pruebas funcionales.');
    }

    const testSummary = parseJUnitResults(TEST_RESULTS_DIR);
    const testSuccess = gradleSuccess && testSummary.failures === 0 && testSummary.errors === 0 && testSummary.totalTests > 0;
    const status = testSuccess ? 'EXITOSO' : 'FALLIDO';

    const julesFeedback = await invokeJulesAgent(
        'Evalúa el comportamiento funcional externo, contratos Retrofit y cálculos comerciales.',
        { testSuccess, testSummary }
    );

    return {
        name: 'Pruebas Funcionales y Contratos Móviles con Jules',
        status,
        testSuccess,
        testSummary,
        julesFeedback
    };
}

// --- 7. Ejecutor 4: Pruebas de Regresión y Estabilidad para App Móvil ---
async function runMobileRegressionAnalysis(scope) {
    console.log('\n--- [4] EJECUTANDO PRUEBAS DE REGRESIÓN REALES CON GRADLE ---');
    const gradlewCmd = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew';
    clearTestResults(TEST_RESULTS_DIR);

    let gradleSuccess = true;
    let rawOutput = '';
    try {
        console.log('Ejecutando suite de regresión: FoodJetRegressionTest...');
        rawOutput = execSync(`${gradlewCmd} testDebugUnitTest --tests "com.example.foodjeetapp.regression.*" --no-daemon --stacktrace`, {
            encoding: 'utf8',
            cwd: WORKSPACE_DIR,
            timeout: 180000
        });
        console.log('✅ Gradle suite de regresión finalizado.');
    } catch (err) {
        gradleSuccess = false;
        rawOutput = (err.stdout || '') + '\n' + (err.stderr || '');
        console.error('❌ Fallo detectado en la suite de pruebas de regresión.');
    }

    const testSummary = parseJUnitResults(TEST_RESULTS_DIR);
    const testSuccess = gradleSuccess && testSummary.failures === 0 && testSummary.errors === 0 && testSummary.totalTests > 0;
    const status = testSuccess ? 'EXITOSO' : 'FALLIDO';

    const julesFeedback = await invokeJulesAgent(
        'Evalúa la no degradación de funcionalidades en modelos conexos de la app móvil.',
        { testSuccess, testSummary }
    );

    return {
        name: 'Pruebas de Regresión y Estabilidad Móvil con Jules',
        status,
        testSuccess,
        testSummary,
        julesFeedback
    };
}

// --- 8. Generación del Reporte Markdown Consolidado ---
function buildMarkdownReport(result, scope) {
    const icon = result.status === 'EXITOSO' ? '🟢' : (result.status === 'ADVERTENCIA' ? '🟡' : '🔴');
    const timestamp = new Date().toISOString();

    let md = `<!-- jules-mobile-report-${TEST_TYPE} -->\n`;
    md += `## ${icon} Agente Jules — Reporte de ${result.name}\n\n`;
    md += `> **Estado general:** **${result.status}** | **Fecha:** \`${timestamp}\` | **Commit:** \`${COMMIT_SHA.substring(0, 7) || 'HEAD'}\`\n\n`;

    md += `### 📱 Delimitación del Radio de Acción Móvil (Git Diff)\n`;
    if (scope.changedFiles.length > 0) {
        md += `Archivos de la aplicación móvil analizados dentro del diferencial de cambios:\n`;
        scope.changedFiles.slice(0, 10).forEach(f => {
            md += `- \`${f}\`\n`;
        });
        if (scope.changedFiles.length > 10) {
            md += `- *...y ${scope.changedFiles.length - 10} archivos más.* \n`;
        }
    } else {
        md += `*No se detectaron archivos modificados en el rango analizado.*\n`;
    }
    md += `\n`;

    md += `### 📊 Métricas y Resultados Reales de Ejecución\n`;
    if (TEST_TYPE === 'static') {
        md += `| Métrica Móvil | Valor |\n|---|---|\n`;
        md += `| Estado de Compilación Kotlin | ${result.metrics.compilacionExitosa} |\n`;
        md += `| Archivos Kotlin y Recursos Auditados | ${result.metrics.archivosAnalizados} |\n`;
        md += `| Vulnerabilidades Críticas / Altas (OWASP Mobile) | ${result.metrics.hallazgosCriticos + result.metrics.hallazgosAltos} |\n`;
        md += `| Advertencias de Calidad y Rendimiento | ${result.metrics.hallazgosMedios + result.metrics.hallazgosBajos} |\n\n`;

        if (result.findings.length > 0) {
            md += `#### 🔍 Detalle de Hallazgos Detectados en la App Móvil:\n\n`;
            md += `| Severidad | Archivo | Línea | Categoría | Descripción |\n|---|---|---|---|---|\n`;
            result.findings.forEach(f => {
                md += `| **${f.severity}** | \`${f.file}\` | ${f.line || '-'} | ${f.category} | ${f.description} |\n`;
            });
            md += `\n`;
        }
    } else {
        const s = result.testSummary;
        md += `| Métrica de Prueba Real (Gradle / JUnit) | Valor |\n|---|---|\n`;
        md += `| Pruebas Ejecutadas | **${s.totalTests}** |\n`;
        md += `| Pruebas Aprobadas (Passed) | **${s.passed}** ✅ |\n`;
        md += `| Pruebas Fallidas (Failures) | **${s.failures}** ${s.failures > 0 ? '❌' : '✅'} |\n`;
        md += `| Errores de Ejecución (Errors) | **${s.errors}** ${s.errors > 0 ? '❌' : '✅'} |\n`;
        md += `| Pruebas Omitidas (Skipped) | ${s.skipped} |\n`;
        md += `| Tiempo de Ejecución en JVM | ${s.timeSeconds.toFixed(3)} s |\n\n`;

        if (s.failedCases.length > 0) {
            md += `#### ❌ Detalle de Pruebas Fallidas Detectadas:\n\n`;
            md += `| Clase de Prueba | Caso de Prueba | Mensaje de Falla |\n|---|---|---|\n`;
            s.failedCases.forEach(fc => {
                md += `| \`${fc.className.split('.').pop()}\` | \`${fc.name}\` | **${fc.failureMessage}** |\n`;
            });
            md += `\n`;
        }

        if (s.testCases.length > 0) {
            md += `#### 🧪 Casos de Prueba Evaluados:\n`;
            s.testCases.forEach(c => {
                const iconCase = c.status === 'EXITOSO' ? '✅' : '❌';
                md += `- ${iconCase} \`${c.className.split('.').pop()}.${c.name}\` (${c.time.toFixed(3)}s)\n`;
            });
            md += `\n`;
        }
    }

    md += `### 🤖 Dictamen del Agente Jules para Móviles\n`;
    if (result.julesFeedback) {
        md += `${result.julesFeedback}\n\n`;
    } else {
        if (result.status === 'EXITOSO') {
            md += `El agente Jules auditó la ejecución completa en el motor Gradle y compilador Kotlin.\n`;
            md += `- **Conclusión:** Todos los criterios técnicos y pruebas automáticas para **${result.name}** han concluido de manera exitosa y conforme.\n`;
            md += `- **Siguiente paso:** Proceder con la integración y despliegue del componente móvil.\n\n`;
        } else {
            md += `El agente Jules detectó inconformidades estrictas que impiden la fusión del código:\n`;
            md += `- **Conclusión:** Se identificaron fallas críticas o asertos no cumplidos en **${result.name}**.\n`;
            md += `- **Acción requerida:** Corregir las aserciones o lógica defectuosa antes de autorizar la integración a la rama principal.\n\n`;
        }
    }

    md += `---\n*Reporte generado automáticamente por Jules Mobile Test Orchestrator.*`;
    return md;
}

// --- 9. Publicación del reporte en Pull Request y Step Summary ---
async function publishReport(markdownContent) {
    if (SUMMARY_FILE) {
        try {
            fs.appendFileSync(SUMMARY_FILE, markdownContent + '\n\n', 'utf8');
            console.log('✅ Reporte consolidado guardado en GITHUB_STEP_SUMMARY.');
        } catch (err) {
            console.error('Error al escribir en GITHUB_STEP_SUMMARY:', err.message);
        }
    }

    if (PR_NUMBER && GITHUB_TOKEN && GITHUB_REPOSITORY) {
        try {
            console.log(`Buscando comentarios previos de Jules en el PR #${PR_NUMBER}...`);
            const commentsUrl = `https://api.github.com/repos/${GITHUB_REPOSITORY}/issues/${PR_NUMBER}/comments`;
            const listRes = await fetch(commentsUrl, {
                headers: {
                    'Authorization': `Bearer ${GITHUB_TOKEN}`,
                    'User-Agent': 'Jules-Mobile-Agent',
                    'Accept': 'application/vnd.github+json'
                }
            });

            let existingCommentId = null;
            if (listRes.ok) {
                const comments = await listRes.json();
                const marker = `<!-- jules-mobile-report-${TEST_TYPE} -->`;
                const found = comments.find(c => c.body && c.body.includes(marker));
                if (found) existingCommentId = found.id;
            }

            if (existingCommentId) {
                console.log(`Actualizando comentario existente #${existingCommentId}...`);
                await fetch(`https://api.github.com/repos/${GITHUB_REPOSITORY}/issues/comments/${existingCommentId}`, {
                    method: 'PATCH',
                    headers: {
                        'Authorization': `Bearer ${GITHUB_TOKEN}`,
                        'User-Agent': 'Jules-Mobile-Agent',
                        'Accept': 'application/vnd.github+json',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ body: markdownContent })
                });
                console.log('✅ Comentario de PR actualizado exitosamente.');
            } else {
                console.log(`Creando nuevo comentario en PR #${PR_NUMBER}...`);
                await fetch(commentsUrl, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${GITHUB_TOKEN}`,
                        'User-Agent': 'Jules-Mobile-Agent',
                        'Accept': 'application/vnd.github+json',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ body: markdownContent })
                });
                console.log('✅ Comentario de PR publicado exitosamente.');
            }
        } catch (err) {
            console.error('⚠️ No se pudo publicar el comentario en el PR:', err.message);
        }
    }
}

// --- Función Principal ---
async function main() {
    console.log(`🚀 Iniciando Jules Mobile Test Orchestrator | Tipo: ${TEST_TYPE}`);
    const scope = getGitDiffScope();
    console.log(`Archivos detectados en radio de acción: ${scope.changedFiles.length}`);

    let result;
    switch (TEST_TYPE) {
        case 'static':
            result = await runMobileStaticAnalysis(scope);
            break;
        case 'unit':
            result = await runMobileUnitWhiteboxAnalysis(scope);
            break;
        case 'functional':
            result = await runMobileFunctionalBlackboxAnalysis(scope);
            break;
        case 'regression':
            result = await runMobileRegressionAnalysis(scope);
            break;
        default:
            console.error(`Tipo de prueba desconocido: ${TEST_TYPE}`);
            process.exit(1);
    }

    const report = buildMarkdownReport(result, scope);
    console.log('\n' + report + '\n');
    await publishReport(report);

    if (result.status === 'FALLIDO') {
        console.error(`❌ La suite de ${result.name} ha finalizado con estado FALLIDO.`);
        process.exit(1);
    } else {
        console.log(`🎉 La suite de ${result.name} ha finalizado con éxito.`);
    }
}

main().catch(err => {
    console.error('Error fatal en orquestador móvil de Jules:', err);
    process.exit(1);
});
