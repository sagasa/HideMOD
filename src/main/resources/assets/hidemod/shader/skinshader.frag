#version 330
// 出力カラー
layout(location = 0) out vec4 out_FragColor;

varying vec4 test_color;

void main(void) {
    // 緑色を出力(RGBA)
    out_FragColor = vec4(test_color.x, test_color.y, test_color.z, 1.0);
}