sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Verification : Screen("verification")
    object Membership : Screen("membership")
}