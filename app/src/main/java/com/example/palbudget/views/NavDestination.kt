package com.example.palbudget.views

sealed class NavDestination {
    object Scan : NavDestination()
    object Receipts : NavDestination()
}
