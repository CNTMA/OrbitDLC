#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location=0) in vec2 vTexCoord;
layout(location=0) out vec4 fragColor;
uniform sampler2D Sampler0;
layout(std140) uniform GroupData { vec4 uGroup; };
void main(){vec4 sampleColor=texture(Sampler0,vTexCoord);bool friendPixel=sampleColor.g>sampleColor.r+0.2&&sampleColor.g>sampleColor.b+0.2;bool itemPixel=sampleColor.b>sampleColor.r+0.2&&sampleColor.b>sampleColor.g+0.2;float type=friendPixel?1.0:(itemPixel?2.0:0.0);bool includePixel=abs(uGroup.x-type)<0.5;fragColor=vec4(1.0,1.0,1.0,includePixel?sampleColor.a:0.0);}
