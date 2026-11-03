package com.example.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GithubIcon: ImageVector
  get() {
    if (_githubIcon != null) return _githubIcon!!
    _githubIcon = ImageVector.Builder(
      name = "Github",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.477f, 2f, 2f, 6.484f, 2f, 12.017f)
        curveTo(2f, 16.446f, 4.843f, 20.198f, 8.805f, 21.523f)
        curveTo(9.305f, 21.615f, 9.488f, 21.306f, 9.488f, 21.037f)
        curveTo(9.488f, 20.796f, 9.479f, 20.158f, 9.474f, 19.31f)
        curveTo(6.692f, 19.921f, 6.105f, 17.949f, 6.105f, 17.949f)
        curveTo(5.651f, 16.78f, 5.0f, 16.469f, 5.0f, 16.469f)
        curveTo(4.093f, 15.84f, 5.069f, 15.852f, 5.069f, 15.852f)
        curveTo(6.071f, 15.923f, 6.6f, 16.896f, 6.6f, 16.896f)
        curveTo(7.492f, 18.444f, 8.938f, 17.997f, 9.508f, 17.738f)
        curveTo(9.599f, 17.085f, 9.857f, 16.639f, 10.143f, 16.386f)
        curveTo(7.922f, 16.131f, 5.586f, 15.261f, 5.586f, 11.388f)
        curveTo(5.586f, 10.285f, 5.974f, 9.382f, 6.611f, 8.674f)
        curveTo(6.508f, 8.419f, 6.165f, 7.391f, 6.709f, 6.007f)
        curveTo(6.709f, 6.007f, 7.548f, 5.736f, 9.458f, 7.042f)
        curveTo(10.255f, 6.818f, 11.099f, 6.706f, 11.94f, 6.702f)
        curveTo(12.78f, 6.706f, 13.625f, 6.818f, 14.423f, 7.042f)
        curveTo(16.331f, 5.736f, 17.168f, 6.007f, 17.168f, 6.007f)
        curveTo(17.714f, 7.391f, 17.371f, 8.419f, 17.269f, 8.674f)
        curveTo(17.908f, 9.382f, 18.293f, 10.285f, 18.293f, 11.388f)
        curveTo(18.293f, 15.272f, 15.952f, 16.128f, 13.723f, 16.377f)
        curveTo(14.083f, 16.69f, 14.407f, 17.311f, 14.407f, 18.261f)
        curveTo(14.407f, 19.63f, 14.394f, 20.735f, 14.394f, 21.037f)
        curveTo(14.394f, 21.309f, 14.574f, 21.621f, 15.083f, 21.521f)
        curveTo(19.041f, 20.194f, 21.881f, 16.444f, 21.881f, 12.017f)
        curveTo(21.881f, 6.484f, 17.408f, 2f, 12f, 2f)
        close()
      }
    }.build()
    return _githubIcon!!
  }

private var _githubIcon: ImageVector? = null

@Composable
fun GithubVectorIcon(
  modifier: Modifier = Modifier,
  tint: Color = Color.Unspecified
) {
  Icon(
    imageVector = GithubIcon,
    contentDescription = "GitHub",
    tint = tint,
    modifier = modifier
  )
}
