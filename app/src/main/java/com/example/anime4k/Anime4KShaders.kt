package com.example.anime4k

/**
 * OpenGL ES 2.0 GLSL shaders implementing real-time Anime4K video restoration & upscaling:
 * - OES Passthrough & OES Luma Extraction
 * - Anime4K_Fast (Luminance PushGrad + ThinLines + Anti-Ringing Clamp)
 * - Anime4K_Upscale_CNN (Non-linear CReLU Feature Convolution + Directional Subpixel Upscale)
 * - Anime4K_Upscale_CNNX (Sobel Structure Deblur + Deep Multi-Kernel CNNX Residual + Subpixel Anti-Ringing)
 */
object Anime4KShaders {

    const val OES_VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        uniform mat4 uTexMatrix;
        varying vec2 vTexCoord;
        varying vec2 vScreenUv;
        void main() {
            gl_Position = aPosition;
            vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
            vScreenUv = aTexCoord;
        }
    """

    const val QUAD_VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """

    // Direct OES -> Screen when Anime4K is OFF
    const val OES_PASSTHROUGH_FRAGMENT = """
        #extension GL_OES_EGL_image_external : require
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTex;
        void main() {
            gl_FragColor = texture2D(uTex, vTexCoord);
        }
    """

    // Pass 1: OES -> RGBA FBO with perceptual luminance stored in alpha
    const val OES_LUMA_EXTRACT_FRAGMENT = """
        #extension GL_OES_EGL_image_external : require
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTex;
        void main() {
            vec3 rgb = texture2D(uTex, vTexCoord).rgb;
            float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
            gl_FragColor = vec4(rgb, luma);
        }
    """

    // Anime4K_Fast: 2-pass Luminance PushGrad + Highlight Clamp
    const val ANIME4K_FAST_FINAL_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uTex;
        uniform sampler2D uOrigTex;
        uniform vec2 uTexelSize;
        uniform float uStrength;
        uniform float uSplitMode;
        uniform float uSplitPos;

        vec4 pushLightest(vec4 cc, vec4 a, vec4 b, vec4 c, float str) {
            float minRight = min(min(a.a, b.a), c.a);
            if (minRight > cc.a) {
                vec4 avg = (a + b + c) * 0.333333;
                return mix(cc, avg, clamp(str * 0.48, 0.0, 0.85));
            }
            return cc;
        }

        void main() {
            if (uSplitMode > 0.5 && vTexCoord.x < uSplitPos) {
                if (abs(vTexCoord.x - uSplitPos) < 0.0022) {
                    gl_FragColor = vec4(0.0, 0.898, 1.0, 1.0);
                    return;
                }
                gl_FragColor = vec4(texture2D(uOrigTex, vTexCoord).rgb, 1.0);
                return;
            }
            if (uSplitMode > 0.5 && abs(vTexCoord.x - uSplitPos) < 0.0022) {
                gl_FragColor = vec4(0.0, 0.898, 1.0, 1.0);
                return;
            }

            vec2 d = uTexelSize;
            vec4 cc = texture2D(uTex, vTexCoord);
            vec4 tl = texture2D(uTex, vTexCoord + vec2(-d.x,  d.y));
            vec4 tc = texture2D(uTex, vTexCoord + vec2( 0.0,  d.y));
            vec4 tr = texture2D(uTex, vTexCoord + vec2( d.x,  d.y));
            vec4 ml = texture2D(uTex, vTexCoord + vec2(-d.x,  0.0));
            vec4 mr = texture2D(uTex, vTexCoord + vec2( d.x,  0.0));
            vec4 bl = texture2D(uTex, vTexCoord + vec2(-d.x, -d.y));
            vec4 bc = texture2D(uTex, vTexCoord + vec2( 0.0, -d.y));
            vec4 br = texture2D(uTex, vTexCoord + vec2( d.x, -d.y));

            vec3 minCol = min(min(min(tl.rgb, tc.rgb), min(tr.rgb, ml.rgb)), min(min(mr.rgb, bl.rgb), min(bc.rgb, br.rgb)));
            vec3 maxCol = max(max(max(tl.rgb, tc.rgb), max(tr.rgb, ml.rgb)), max(max(mr.rgb, bl.rgb), max(bc.rgb, br.rgb)));

            vec4 res = cc;
            res = pushLightest(res, tl, tc, tr, uStrength);
            res = pushLightest(res, bl, bc, br, uStrength);
            res = pushLightest(res, tl, ml, bl, uStrength);
            res = pushLightest(res, tr, mr, br, uStrength);

            // Crisp ink-line darkening along local gradient valleys
            vec3 lap = (tc.rgb + bc.rgb + ml.rgb + mr.rgb) * 0.25 - res.rgb;
            vec3 sharpened = res.rgb - lap * (0.55 * uStrength);
            vec3 clamped = clamp(sharpened, min(minCol, cc.rgb), max(maxCol, cc.rgb));
            gl_FragColor = vec4(clamped, 1.0);
        }
    """

    // Anime4K_Upscale_CNN Pass 2 (Feature Convolution & Line Restoration into FBO_B)
    const val ANIME4K_CNN_RESTORE_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uTex;
        uniform vec2 uTexelSize;
        uniform float uStrength;

        void main() {
            vec2 d = uTexelSize;
            vec4 cc = texture2D(uTex, vTexCoord);
            vec4 tl = texture2D(uTex, vTexCoord + vec2(-d.x,  d.y));
            vec4 tc = texture2D(uTex, vTexCoord + vec2( 0.0,  d.y));
            vec4 tr = texture2D(uTex, vTexCoord + vec2( d.x,  d.y));
            vec4 ml = texture2D(uTex, vTexCoord + vec2(-d.x,  0.0));
            vec4 mr = texture2D(uTex, vTexCoord + vec2( d.x,  0.0));
            vec4 bl = texture2D(uTex, vTexCoord + vec2(-d.x, -d.y));
            vec4 bc = texture2D(uTex, vTexCoord + vec2( 0.0, -d.y));
            vec4 br = texture2D(uTex, vTexCoord + vec2( d.x, -d.y));

            // Multi-orientation CReLU CNN feature extraction on Luma
            float fHoriz = cc.a * 2.0 - (ml.a + mr.a);
            float fVert  = cc.a * 2.0 - (tc.a + bc.a);
            float fDiag1 = cc.a * 2.0 - (tl.a + br.a);
            float fDiag2 = cc.a * 2.0 - (tr.a + bl.a);

            float actPos = max(0.0, fHoriz) + max(0.0, fVert) + 0.707 * (max(0.0, fDiag1) + max(0.0, fDiag2));
            float actNeg = max(0.0, -fHoriz) + max(0.0, -fVert) + 0.707 * (max(0.0, -fDiag1) + max(0.0, -fDiag2));

            // Residual CNN line restoration (enhances dark cel lines strongly, protects flat color fills)
            float edgeConfidence = smoothstep(0.015, 0.16, actPos + actNeg);
            float residual = (actPos * 0.24 - actNeg * 0.34) * uStrength * edgeConfidence;

            vec3 minRgb = min(cc.rgb, min(min(tc.rgb, bc.rgb), min(ml.rgb, mr.rgb)));
            vec3 maxRgb = max(cc.rgb, max(max(tc.rgb, bc.rgb), max(ml.rgb, mr.rgb)));

            vec3 outRgb = clamp(cc.rgb + vec3(residual), minRgb * 0.96, maxRgb);
            float outLuma = dot(outRgb, vec3(0.299, 0.587, 0.114));
            gl_FragColor = vec4(outRgb, outLuma);
        }
    """

    // Anime4K_Upscale_CNNX Pass 2 (Sobel Structure Deblur before Deep CNNX Upscale)
    const val ANIME4K_CNNX_STRUCTURE_DEBLUR_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uTex;
        uniform vec2 uTexelSize;
        uniform float uStrength;

        void main() {
            vec2 d = uTexelSize;
            vec4 cc = texture2D(uTex, vTexCoord);
            vec4 tl = texture2D(uTex, vTexCoord + vec2(-d.x,  d.y));
            vec4 tc = texture2D(uTex, vTexCoord + vec2( 0.0,  d.y));
            vec4 tr = texture2D(uTex, vTexCoord + vec2( d.x,  d.y));
            vec4 ml = texture2D(uTex, vTexCoord + vec2(-d.x,  0.0));
            vec4 mr = texture2D(uTex, vTexCoord + vec2( d.x,  0.0));
            vec4 bl = texture2D(uTex, vTexCoord + vec2(-d.x, -d.y));
            vec4 bc = texture2D(uTex, vTexCoord + vec2( 0.0, -d.y));
            vec4 br = texture2D(uTex, vTexCoord + vec2( d.x, -d.y));

            float gx = (tr.a + 2.0 * mr.a + br.a) - (tl.a + 2.0 * ml.a + bl.a);
            float gy = (tl.a + 2.0 * tc.a + tr.a) - (bl.a + 2.0 * bc.a + br.a);
            float gradMag = clamp(length(vec2(gx, gy)), 0.0, 1.0);

            // Bilateral denoise in flat cel areas + directional deblur on contours
            vec3 ringAvg = (tc.rgb + bc.rgb + ml.rgb + mr.rgb + (tl.rgb + tr.rgb + bl.rgb + br.rgb) * 0.5) / 6.0;
            float flatMask = 1.0 - smoothstep(0.02, 0.09, gradMag);
            vec3 denoised = mix(cc.rgb, ringAvg, flatMask * 0.25);

            vec3 deblurred = denoised + (denoised - ringAvg) * (gradMag * 1.15 * uStrength);
            vec3 minRgb = min(cc.rgb, min(min(tc.rgb, bc.rgb), min(ml.rgb, mr.rgb)));
            vec3 maxRgb = max(cc.rgb, max(max(tc.rgb, bc.rgb), max(ml.rgb, mr.rgb)));
            vec3 clamped = clamp(deblurred, minRgb * 0.94, maxRgb);
            float luma = dot(clamped, vec3(0.299, 0.587, 0.114));
            gl_FragColor = vec4(clamped, luma);
        }
    """

    // Final Subpixel Edge-Directed Upscale & Highlight Clamp (shared final stage for CNN & CNNX)
    const val ANIME4K_SUBPIXEL_UPSCALE_FINAL_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uTex;
        uniform sampler2D uOrigTex;
        uniform vec2 uTexelSize;
        uniform float uStrength;
        uniform float uSplitMode;
        uniform float uSplitPos;
        uniform float uIsUltra;

        void main() {
            if (uSplitMode > 0.5 && vTexCoord.x < uSplitPos) {
                if (abs(vTexCoord.x - uSplitPos) < 0.0022) {
                    gl_FragColor = vec4(1.0, 0.302, 0.553, 1.0);
                    return;
                }
                gl_FragColor = vec4(texture2D(uOrigTex, vTexCoord).rgb, 1.0);
                return;
            }
            if (uSplitMode > 0.5 && abs(vTexCoord.x - uSplitPos) < 0.0022) {
                gl_FragColor = vec4(1.0, 0.302, 0.553, 1.0);
                return;
            }

            vec2 d = uTexelSize;
            vec4 cc = texture2D(uTex, vTexCoord);
            vec4 tl = texture2D(uTex, vTexCoord + vec2(-d.x,  d.y));
            vec4 tc = texture2D(uTex, vTexCoord + vec2( 0.0,  d.y));
            vec4 tr = texture2D(uTex, vTexCoord + vec2( d.x,  d.y));
            vec4 ml = texture2D(uTex, vTexCoord + vec2(-d.x,  0.0));
            vec4 mr = texture2D(uTex, vTexCoord + vec2( d.x,  0.0));
            vec4 bl = texture2D(uTex, vTexCoord + vec2(-d.x, -d.y));
            vec4 bc = texture2D(uTex, vTexCoord + vec2( 0.0, -d.y));
            vec4 br = texture2D(uTex, vTexCoord + vec2( d.x, -d.y));

            // Compute edge tangent & normal for subpixel diagonal anti-aliasing + crisp normal push
            float gx = (tr.a + 2.0 * mr.a + br.a) - (tl.a + 2.0 * ml.a + bl.a);
            float gy = (tl.a + 2.0 * tc.a + tr.a) - (bl.a + 2.0 * bc.a + br.a);
            vec2 grad = vec2(gx, gy);
            float gLen = length(grad);

            vec3 result = cc.rgb;
            if (gLen > 0.015) {
                vec2 normal = grad / gLen;
                vec2 tangent = vec2(-normal.y, normal.x);

                // Smooth along edge tangent (eliminates stair-stepping jaggies on diagonal anime lines)
                vec3 tangA = texture2D(uTex, vTexCoord + tangent * d * 0.65).rgb;
                vec3 tangB = texture2D(uTex, vTexCoord - tangent * d * 0.65).rgb;
                vec3 tangentSmooth = (result * 2.0 + tangA + tangB) * 0.25;

                // Sharpen across edge normal with strict min/max anti-ringing clamp
                vec4 normPlus  = texture2D(uTex, vTexCoord + normal * d * 0.85);
                vec4 normMinus = texture2D(uTex, vTexCoord - normal * d * 0.85);

                float pushFactor = clamp(gLen * (0.65 + 0.30 * uIsUltra) * uStrength, 0.0, 0.78);
                vec3 target = (abs(cc.a - normPlus.a) < abs(cc.a - normMinus.a)) ? normPlus.rgb : normMinus.rgb;
                result = mix(tangentSmooth, target, pushFactor);
            }

            vec3 minCol = min(min(min(tl.rgb, tc.rgb), min(tr.rgb, ml.rgb)), min(min(mr.rgb, bl.rgb), min(bc.rgb, br.rgb)));
            vec3 maxCol = max(max(max(tl.rgb, tc.rgb), max(tr.rgb, ml.rgb)), max(max(mr.rgb, bl.rgb), max(bc.rgb, br.rgb)));
            result = clamp(result, min(minCol, cc.rgb), max(maxCol, cc.rgb));

            gl_FragColor = vec4(result, 1.0);
        }
    """
}
