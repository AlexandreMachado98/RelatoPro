$mainActivity = Get-Content app/src/main/java/com/relatopro/app/MainActivity.kt -Raw

$replaceRoute = @"
                            composable(
                                route = "my_reports?filter={filter}",
                                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "Todos" })
                            ) { backStackEntry ->
                                val filter = backStackEntry.arguments?.getString("filter") ?: "Todos"
                                MyReportsScreen(
                                    viewModel = hiltViewModel(),
                                    initialFilter = filter,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
"@
$mainActivity = $mainActivity -replace 'composable\("my_reports"\) \{\s*MyReportsScreen\(\s*viewModel = hiltViewModel\(\),\s*onNavigateBack = \{ navController.popBackStack\(\) \}\s*\)\s*\}', $replaceRoute

Set-Content app/src/main/java/com/relatopro/app/MainActivity.kt $mainActivity
Write-Host "Updated MainActivity routes"
