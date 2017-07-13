attribute vec4 a_Position;
attribute vec4 a_TextureCoord;

varying vec2 v_TextureCoord;

uniform mat4 u_STMatrix;

void main() {
    gl_Position = a_Position;
    v_TextureCoord = (u_STMatrix * a_TextureCoord).xy;
}
