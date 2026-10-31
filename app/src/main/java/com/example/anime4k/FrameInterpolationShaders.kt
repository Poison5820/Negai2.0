package com.example.anime4k

/**
 * OpenGL ES 2.0 shaders for artifact-free phase-locked MEMC (Motion Estimation & Motion Compensation)
 * frame interpolation.
 *
 * Conservative Anime Contour Protection Design:
 * 1. [MEMC_MOTION_ESTIMATE_FRAGMENT] (1/4-res): Multi-scale concentric search with conservative 0.025 UV
 *    motion clamp, Rate-Distortion zero-motion penalty, and diamond sub-texel refinement.
 * 2. [MEMC_MOTION_SMOOTH_FRAGMENT] (1/4-res): 3x3 bilateral edge-preserving vector filter + true scene-cut detector.
 * 3. [MEMC_SYNTHESIZE_FRAGMENT] (full-res, per VSync):
 *    - Spatial line-art gradient detection on source frames: on high-contrast contours, motion vector displacement
 *      is automatically damped to prevent line tearing and rubber-morphing.
 *    - Adaptive fallback: when warped frames diverge or confidence is low, safely transitions toward clean native linear blending.
 *    - Strict color bounding box: synthesized color is clamped strictly within [boxMin, boxMax] of original frames,
 *      100% eliminating ghost halos and color fringing.
 */
object FrameInterpolationShaders {

    /**
     * Maximum displacement in UV space encoded by motion channels [0..1] around 128/255.
     * Conservative 0.025 limit prevents rubber/gel morphing on 2D anime character line art.
     */
    const val MAX_MOTION_UV = 0.025f

    const val MEMC_MOTION_ESTIMATE_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uPrevTex;
        uniform sampler2D uCurrTex;
        uniform vec2 uTexelSize;
        uniform float uSearchRadius;

        const float ZERO_MV = 128.0 / 255.0;
        const float MAX_MV = 0.025;

        // Perceptually weighted color difference in [0..1]
        float colorDiff(vec3 a, vec3 b) {
            vec3 d = abs(a - b);
            return dot(d, vec3(0.299, 0.587, 0.114));
        }

        // 5-tap symmetric patch difference around vTexCoord with displacement mv
        float patchSad(vec2 mv) {
            vec2 pUv = clamp(vTexCoord - mv * 0.5, vec2(0.002), vec2(0.998));
            vec2 cUv = clamp(vTexCoord + mv * 0.5, vec2(0.002), vec2(0.998));
            vec2 dx = vec2(uTexelSize.x * 1.5, 0.0);
            vec2 dy = vec2(0.0, uTexelSize.y * 1.5);

            float s = colorDiff(texture2D(uPrevTex, pUv).rgb, texture2D(uCurrTex, cUv).rgb) * 2.0;
            s += colorDiff(texture2D(uPrevTex, pUv - dx).rgb, texture2D(uCurrTex, cUv - dx).rgb);
            s += colorDiff(texture2D(uPrevTex, pUv + dx).rgb, texture2D(uCurrTex, cUv + dx).rgb);
            s += colorDiff(texture2D(uPrevTex, pUv - dy).rgb, texture2D(uCurrTex, cUv - dy).rgb);
            s += colorDiff(texture2D(uPrevTex, pUv + dy).rgb, texture2D(uCurrTex, cUv + dy).rgb);
            return s * 0.166667;
        }

        // Unit search directions
        vec2 getDir(int idx) {
            if (idx == 0) return vec2( 1.0,  0.0);
            if (idx == 1) return vec2(-1.0,  0.0);
            if (idx == 2) return vec2( 0.0,  1.0);
            if (idx == 3) return vec2( 0.0, -1.0);
            if (idx == 4) return vec2( 0.7071,  0.7071);
            if (idx == 5) return vec2(-0.7071,  0.7071);
            if (idx == 6) return vec2( 0.7071, -0.7071);
            return vec2(-0.7071, -0.7071);
        }

        void main() {
            vec2 stepUnit = uTexelSize * max(uSearchRadius, 1.0);
            float zeroSad = patchSad(vec2(0.0));

            // Instant early-out for static or flat background regions (zero boiling noise)
            if (zeroSad < 0.0035) {
                gl_FragColor = vec4(ZERO_MV, ZERO_MV, 1.0, zeroSad);
                return;
            }

            // Zero-motion prior / Rate-Distortion lambda penalty
            // Non-zero candidates must overcome their displacement penalty to be chosen.
            float lambda = 0.16;
            vec2 bestMv = vec2(0.0);
            float bestCost = zeroSad;
            float bestSad = zeroSad;

            // Multi-scale concentric search:
            // Scale 1: Fine motion (1.0 * stepUnit)
            for (int i = 0; i < 8; i++) {
                vec2 cand = getDir(i) * (stepUnit * 1.0);
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Scale 2: Medium motion (2.0 * stepUnit)
            for (int i = 0; i < 8; i++) {
                vec2 cand = getDir(i) * (stepUnit * 2.0);
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Sub-texel diamond refinement around best candidate
            vec2 refStep = stepUnit * 0.45;
            for (int j = 0; j < 4; j++) {
                vec2 refined = bestMv + getDir(j) * refStep;
                float sad = patchSad(refined);
                float cost = sad + length(refined) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = refined;
                }
            }

            // Clamp motion vector into conservative MAX_MV range
            float mvLen = length(bestMv);
            if (mvLen > MAX_MV) {
                bestMv = (bestMv / mvLen) * MAX_MV;
            }

            // Confidence metric: 1.0 means clear unambiguous match; lower means ambiguous
            float confidence = clamp((zeroSad - bestSad) / max(zeroSad, 0.01) * 2.0, 0.0, 1.0);

            // Channel encoding:
            // .r = X displacement in [0..1] centered at ZERO_MV (128/255)
            // .g = Y displacement in [0..1] centered at ZERO_MV (128/255)
            // .b = Match confidence in [0..1]
            // .a = Best match SAD in [0..1] for scene cut detection
            vec2 encoded = clamp(ZERO_MV + (bestMv / (2.0 * MAX_MV)), 0.0, 1.0);
            gl_FragColor = vec4(encoded.x, encoded.y, confidence, bestSad);
        }
    """

    const val MEMC_MOTION_SMOOTH_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uMotionTex;
        uniform vec2 uMotionTexel;

        const float ZERO_MV = 128.0 / 255.0;

        void main() {
            vec4 center = texture2D(uMotionTex, vTexCoord);
            vec2 centerMv = center.rg - ZERO_MV;

            // 3x3 bilateral edge-preserving vector filter
            vec2 sumMv = centerMv * (0.8 + center.b * 2.0);
            float weightSum = (0.8 + center.b * 2.0);
            float confSum = center.b;

            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    if (x == 0 && y == 0) continue;
                    vec4 nb = texture2D(uMotionTex, vTexCoord + vec2(float(x), float(y)) * uMotionTexel);
                    vec2 nbMv = nb.rg - ZERO_MV;
                    // Motion similarity: drop weight if neighbor vector points in a completely different direction
                    float sim = 1.0 - smoothstep(0.015, 0.08, length(nbMv - centerMv));
                    float w = (0.4 + nb.b * 1.6) * sim;
                    sumMv += nbMv * w;
                    weightSum += w;
                    confSum += nb.b;
                }
            }

            vec2 smoothedMv = sumMv / max(weightSum, 0.001);
            vec2 smoothEncoded = clamp(ZERO_MV + smoothedMv, 0.0, 1.0);
            float avgConf = confSum / 9.0;

            // True scene-cut detector:
            // In channel .a, each sample holds bestSad (the best matching SAD found).
            // A scene cut occurs ONLY when even the best motion vectors fail to match across the screen!
            float globalMatchSad = 0.0;
            globalMatchSad += texture2D(uMotionTex, vec2(0.20, 0.20)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.50, 0.20)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.80, 0.20)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.20, 0.50)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.50, 0.50)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.80, 0.50)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.20, 0.80)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.50, 0.80)).a;
            globalMatchSad += texture2D(uMotionTex, vec2(0.80, 0.80)).a;
            globalMatchSad /= 9.0;

            // Threshold: best matching SAD > 0.25 means scenes are completely mismatched
            float isSceneCut = step(0.25, globalMatchSad);
            gl_FragColor = vec4(smoothEncoded.x, smoothEncoded.y, avgConf, isSceneCut);
        }
    """

    const val MEMC_SYNTHESIZE_FRAGMENT = """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        varying vec2 vTexCoord;
        uniform sampler2D uPrevTex;
        uniform sampler2D uCurrTex;
        uniform sampler2D uMotionTex;
        uniform vec2 uTexelSize;
        uniform float uAlpha;
        uniform float uStrength;
        uniform float uEdgeProtect;
        uniform int uSplitCompare;
        uniform float uSplitX;

        const float ZERO_MV = 128.0 / 255.0;
        const float MAX_MV = 0.025;

        void main() {
            float t = clamp(uAlpha, 0.0, 1.0);
            vec4 prevBase = texture2D(uPrevTex, vTexCoord);
            vec4 currBase = texture2D(uCurrTex, vTexCoord);

            // Split-Screen comparison: left shows native un-interpolated cadence
            if (uSplitCompare == 1 && vTexCoord.x < uSplitX) {
                gl_FragColor = (t < 0.5) ? prevBase : currBase;
                return;
            }

            // Exact endpoints: 100% native source frames
            if (t <= 0.002) {
                gl_FragColor = prevBase;
                return;
            }
            if (t >= 0.998) {
                gl_FragColor = currBase;
                return;
            }

            vec4 motion = texture2D(uMotionTex, vTexCoord);

            // Clean scene-cut snap: instant transition without morphing
            if (motion.a > 0.5) {
                gl_FragColor = (t < 0.5) ? prevBase : currBase;
                return;
            }

            vec2 rawMv = (motion.rg - ZERO_MV) * (2.0 * MAX_MV) * uStrength;
            float confidence = clamp(motion.b, 0.0, 1.0);

            // Subtitle & Static On-Screen Elements Protection:
            // If the pixel is identical in both original frames, it is static on-screen text/HUD.
            // Force motion to zero so subtitles never wobble or blur.
            float staticDiff = length(prevBase.rgb - currBase.rgb);
            float staticWeight = 1.0 - smoothstep(0.008, 0.032, staticDiff);

            // High-contrast Anime Line Art Contour Detection:
            // Measure spatial luminance gradients in original frames around vTexCoord.
            vec3 pR = texture2D(uPrevTex, vTexCoord + vec2(uTexelSize.x, 0.0)).rgb;
            vec3 pL = texture2D(uPrevTex, vTexCoord - vec2(uTexelSize.x, 0.0)).rgb;
            vec3 pU = texture2D(uPrevTex, vTexCoord + vec2(0.0, uTexelSize.y)).rgb;
            vec3 pD = texture2D(uPrevTex, vTexCoord - vec2(0.0, uTexelSize.y)).rgb;
            float lineGradient = length(pR - pL) + length(pU - pD);

            // Contour protection: on sharp character outlines, damp vector displacement
            // to completely eliminate line tearing, staircasing, and rubber/gel morphing!
            float contourDamp = 1.0 - smoothstep(0.07, 0.25, lineGradient) * 0.80;

            // Confidence damp: if motion estimation was noisy or low-confidence,
            // pull motion toward zero to maintain stability instead of warping into distortion.
            float confDamp = smoothstep(0.12, 0.55, confidence);
            vec2 mv = rawMv * (1.0 - staticWeight) * confDamp * contourDamp;

            // Motion-compensated bidirectional sample coordinates
            vec2 uvPrev = clamp(vTexCoord - mv * t, vec2(0.001), vec2(0.999));
            vec2 uvCurr = clamp(vTexCoord + mv * (1.0 - t), vec2(0.001), vec2(0.999));

            vec3 warpPrev = texture2D(uPrevTex, uvPrev).rgb;
            vec3 warpCurr = texture2D(uCurrTex, uvCurr).rgb;

            // Occlusion Handling (Bidirectional agreement):
            // When an object reveals or covers the background, warpPrev and warpCurr disagree.
            // Instead of cross-dissolving unwarped images (which causes triple ghosting!),
            // adaptively shift the blend weight toward the temporally closer warped frame.
            float warpDisagreement = length(warpPrev - warpCurr);
            float occlusion = smoothstep(0.06, 0.28, warpDisagreement);
            float temporalWeight = mix(t, step(0.5, t), occlusion * 0.75);
            vec3 synthesized = mix(warpPrev, warpCurr, temporalWeight);

            // Safe fallback to clean linear blend:
            // When warped frames diverge significantly, blend safely toward clean native frames
            // rather than showing distorted or torn pixel clusters.
            vec3 safeLinearBlend = mix(prevBase.rgb, currBase.rgb, t);
            float fallbackAlpha = smoothstep(0.08, 0.26, warpDisagreement);
            synthesized = mix(synthesized, safeLinearBlend, fallbackAlpha * 0.80);

            // 2D Anime Contour / Line Art Protection:
            // Clamps the synthesized color into the local bounding box of the source frames.
            // Completely eliminates haloing, color fringing, and edge tearing.
            vec3 boxMin = min(prevBase.rgb, currBase.rgb) - vec3(0.004);
            vec3 boxMax = max(prevBase.rgb, currBase.rgb) + vec3(0.004);
            synthesized = clamp(synthesized, boxMin, boxMax);

            // Split-Screen vertical separator line
            if (uSplitCompare == 1 && abs(vTexCoord.x - uSplitX) < 0.0022) {
                synthesized = mix(synthesized, vec3(0.15, 0.92, 1.0), 0.85);
            }

            gl_FragColor = vec4(synthesized, 1.0);
        }
    """
}
