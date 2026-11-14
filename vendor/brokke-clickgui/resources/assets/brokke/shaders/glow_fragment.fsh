#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uParams;
    vec4 uColor;
};

layout(location = 0) in vec2 vUV;

layout(location = 0) out vec4 fragColor;

void main() {
    float dist = length(vUV);
    
    float normDist = dist * 1.5;
    float glow = exp(-normDist * normDist * 3.0);
    
    float alpha = glow * uColor.a;
    
    fragColor = vec4(uColor.rgb, alpha);
}
