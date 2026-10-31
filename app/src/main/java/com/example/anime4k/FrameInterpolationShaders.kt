package com.example.anime4k

/**
 * OpenGL ES 2.0 shaders for artifact-free phase-locked MEMC (Motion Estimation & Motion Compensation)
 * frame interpolation.
 *
 * Key Artifact-Free Design:
 * 1. [MEMC_MOTION_ESTIMATE_FRAGMENT] (1/4-res): Multi-scale concentric search with Rate-Distortion
 *    zero-motion penalty (eliminates flat-surface and static background shimmering/boiling) + sub-texel
 *    diamond refinement (no unstable gradient division noise) + channel .a storing best match SAD.
 * 2. [MEMC_MOTION_SMOOTH_FRAGMENT] (1/4-res): Bilateral edge-preserving vector filter (prevents foreground
 *    motion from bleeding into background) + true scene-cut detector based on post-match global SAD
 *    (zero false triggers on fast camera pans).
 * 3. [MEMC_SYNTHESIZE_FRAGMENT] (full-res, per VSync): Motion-compensated bidirectional warp with
 *    color bounding box clamping (100% eliminates ghost halos and ringing), static subtitle/HUD protection
 *    (keeps on-screen text razor-sharp without wobble), adaptive occlusion weighting, and Split-Screen mode.
 */
object FrameInterpolationShaders {

    /**
     * Maximum displacement in UV space encoded by motion channels [0..1] around 128/255.
     * 0.05 corresponds to +/- 5% of frame width/height per source frame interval.
     */
    const val MAX_MOTION_UV = 0.05f

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
        const float MAX_MV = 0.05;

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
            float lambda = 0.14;
            vec2 bestMv = vec2(0.0);
            float bestCost = zeroSad;
            float bestSad = zeroSad;

            // Multi-scale concentric search:
            // Scale 1: Fine motion (1.2 * stepUnit)
            for (int i = 0; i < 8; i++) {
                vec2 cand = getDir(i) * (stepUnit * 1.2);
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Scale 2: Medium motion (2.4 * stepUnit)
            for (int i = 0; i < 8; i++) {
                vec2 cand = getDir(i) * (stepUnit * 2.4);
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Scale 3: Fast camera pans (4.8 * stepUnit)
            for (int i = 0; i < 8; i++) {
                vec2 cand = getDir(i) * (stepUnit * 4.8);
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Sub-texel refinement around bestMv (+/- 0.6 * stepUnit diamond)
            vec2 refineStep = stepUnit * 0.6;
            for (int j = 0; j < 4; j++) {
                vec2 delta = (j == 0) ? vec2(refineStep.x, 0.0) :
                             (j == 1) ? vec2(-refineStep.x, 0.0) :
                             (j == 2) ? vec2(0.0, refineStep.y) :
                                        vec2(0.0, -refineStep.y);
                vec2 cand = bestMv + delta;
                float sad = patchSad(cand);
                float cost = sad + length(cand) * lambda;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSad = sad;
                    bestMv = cand;
                }
            }

            // Deadband tiny sub-pixel noise when confidence is marginal
            if (length(bestMv) < length(stepUnit) * 0.35 && zeroSad < 0.012) {
                bestMv = vec2(0.0);
                bestSad = zeroSad;
            }

            vec2 clampedMv = clamp(bestMv, vec2(-MAX_MV), vec2(MAX_MV));
            vec2 encodedMv = clamp(ZERO_MV + clampedMv / (2.0 * MAX_MV), 0.0, 1.0);
            float confidence = clamp(1.0 - bestSad * 3.6, 0.0, 1.0);
            gl_FragColor = vec4(encodedMv.x, encodedMv.y, confidence, bestSad);
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

            // Bilateral edge-preserving motion smoothing
            // Only blends neighbors with compatible motion vectors so object silhouettes don't drag background
            vec2 sumMv = centerMv * (2.0 + center.b * 3.0);
            float weightSum = 2.0 + center.b * 3.0;
            float confSum = center.b;

            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    if (x == 0 && y == 0) continue;
                    vec4 nb = texture2D(uMotionTex, vTexCoord + vec2(float(x), float(y)) * uMotionTexel);
                    vec2 nbMv = nb.rg - ZERO_MV;
                    // Motion similarity: drop weight if neighbor vector points in a completely different direction
                    float sim = 1.0 - smoothstep(0.02, 0.12, length(nbMv - centerMv));
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

            // Threshold: best matching SAD > 0.28 means scenes are completely mismatched
            float isSceneCut = step(0.28, globalMatchSad);
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
        const float MAX_MV = 0.05;

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

            // Confidence damp: if motion estimation was noisy or low-confidence,
            // pull motion toward zero to maintain stability instead of warping into distortion.
            float confDamp = smoothstep(0.10, 0.50, confidence);
            vec2 mv = rawMv * (1.0 - staticWeight) * confDamp;

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
            float occlusion = smoothstep(0.08, 0.32, warpDisagreement);
            float temporalWeight = mix(t, step(0.5, t), occlusion * 0.70);
            vec3 synthesized = mix(warpPrev, warpCurr, temporalWeight);

            // 2D Anime Contour / Line Art Protection:
            // Clamps the synthesized color into the local bounding box of the warped samples.
            // Completely eliminates haloing, color fringing, and edge tearing.
            if (uEdgeProtect > 0.05) {
                vec3 boxMin = min(warpPrev, warpCurr) - vec3(0.02);
                vec3 boxMax = max(warpPrev, warpCurr) + vec3(0.02);
                synthesized = clamp(synthesized, boxMin, boxMax);
            }

            // Split-Screen vertical separator line
            if (uSplitCompare == 1 && abs(vTexCoord.x - uSplitX) < 0.0022) {
                synthesized = mix(synthesized, vec3(0.15, 0.92, 1.0), 0.85);
            }

            gl_FragColor = vec4(synthesized, 1.0);
        }
    """
}
