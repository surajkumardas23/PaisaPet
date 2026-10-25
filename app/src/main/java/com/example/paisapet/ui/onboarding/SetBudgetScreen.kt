package com.example.paisapet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.onboarding.BudgetCheck
import com.example.paisapet.onboarding.BudgetInput
import com.example.paisapet.ui.buddy.BuddyPlaceholder
import com.example.paisapet.ui.theme.BuddyMint
import com.example.paisapet.ui.theme.SurfaceContainer

/**
 * S2 Set misc budget (02-set-budget.png). [text] is digits only, as BudgetInput.accept leaves it;
 * [onTextChange] gets the accepted text after typing, pasting or a chip tap.
 */
@Composable
fun SetBudgetScreen(
    text: String,
    onTextChange: (String) -> Unit,
    saving: Boolean,
    saveFailed: Boolean,
    onBack: () -> Unit,
    onContinue: (budgetPaise: Long) -> Unit,
) {
    val check = BudgetInput.check(text)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .displayCutoutPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        TopBar(onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Text(
                "How much do you want to spend on miscellaneous this month?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "What's left after rent, bills, essentials, and savings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            BudgetAmountCard(text, check, onTextChange)
            Spacer(Modifier.height(20.dp))
            Text(
                "Popular choices",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            BudgetChips(selected = BudgetInput.selectedChip(text), onPick = { onTextChange(it.toString()) })
            Spacer(Modifier.height(20.dp))
            TipCard()
        }
        if (saveFailed) {
            Text(
                "Couldn't save. Try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        PrimaryPillButton(
            text = "Continue",
            enabled = check is BudgetCheck.Valid && !saving,
            onClick = { (check as? BudgetCheck.Valid)?.let { onContinue(it.paise) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
        }
        StepIndicator(current = 1, total = 2, modifier = Modifier.align(Alignment.Center))
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .background(BuddyMint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BuddyPlaceholder(size = 32.dp)
        }
    }
}

@Composable
private fun TipCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(BuddyMint, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BuddyPlaceholder(size = 30.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Buddy's Tip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Don't worry, you can easily change this anytime in Settings as your routine shifts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
