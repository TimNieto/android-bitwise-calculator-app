package com.timnieto.bitwisecalculator;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_EXPRESSION_LENGTH = 60;
    private static final String DEFAULT_DISPLAY = "0";
    private static final String ERROR_MESSAGE = "Syntax Error";
    private static final String INPUT_TOO_LONG_MESSAGE = "Input too long";

    private TextView displayTextView;
    private TextView expressionTextView;
    private final StringBuilder expression = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        applySystemBarPadding();

        displayTextView = findViewById(R.id.tvDisplay);
        expressionTextView = findViewById(R.id.tvExpression);

        setupNumberButtons();
        setupOperatorButtons();
        setupActionButtons();
    }

    private void applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int sidePadding = getResources().getDimensionPixelSize(R.dimen.screen_padding);
            int bottomPadding = getResources().getDimensionPixelSize(R.dimen.bottom_padding);

            view.setPadding(
                    sidePadding + systemBars.left,
                    systemBars.top,
                    sidePadding + systemBars.right,
                    bottomPadding + systemBars.bottom);

            return insets;
        });
    }

    private void setupNumberButtons() {
        int[] numberButtonIds = {
                R.id.btn0,
                R.id.btn1,
                R.id.btn2,
                R.id.btn3,
                R.id.btn4,
                R.id.btn5,
                R.id.btn6,
                R.id.btn7,
                R.id.btn8,
                R.id.btn9
        };

        for (int buttonId : numberButtonIds) {
            findViewById(buttonId)
                    .setOnClickListener(view -> appendToExpression(((TextView) view).getText().toString()));
        }
    }

    private void setupOperatorButtons() {
        setInputButton(R.id.btnAnd, "&");
        setInputButton(R.id.btnOr, "|");
        setInputButton(R.id.btnXor, "^");
        setInputButton(R.id.btnNot, "~");
        setInputButton(R.id.btnLeftShift, "<<");
        setInputButton(R.id.btnRightShift, ">>");
        setInputButton(R.id.btnLeftParen, "(");
        setInputButton(R.id.btnRightParen, ")");
    }

    private void setupActionButtons() {
        findViewById(R.id.btnAC).setOnClickListener(view -> clearExpression());
        findViewById(R.id.btnEqual).setOnClickListener(view -> calculateResult());
    }

    private void setInputButton(int buttonId, String value) {
        findViewById(buttonId).setOnClickListener(view -> appendToExpression(value));
    }

    private void appendToExpression(String value) {
        if (expression.length() + value.length() > MAX_EXPRESSION_LENGTH) {
            showError(INPUT_TOO_LONG_MESSAGE);
            return;
        }

        expression.append(value);
        setDisplayText(expression.toString());
    }

    private void clearExpression() {
        expression.setLength(0);
        expressionTextView.setText("");
        setDisplayText(DEFAULT_DISPLAY);
    }

    private void calculateResult() {
        if (expression.length() == 0) {
            showError(ERROR_MESSAGE);
            return;
        }

        String currentExpression = expression.toString();

        try {
            BitwiseExpressionParser parser = new BitwiseExpressionParser(currentExpression);
            int result = parser.parse();

            expressionTextView.setText(currentExpression);
            setDisplayText(String.valueOf(result));
            expression.setLength(0);
        } catch (IllegalArgumentException exception) {
            showError(ERROR_MESSAGE, currentExpression);
        }
    }

    private void setDisplayText(String text) {
        displayTextView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.display_text_size));
        displayTextView.setText(text);
    }

    private void showError(String message) {
        showError(message, "");
    }

    private void showError(String message, String failedExpression) {
        expression.setLength(0);
        expressionTextView.setText(failedExpression);
        setDisplayText(message);
    }

    private static final class BitwiseExpressionParser {

        private final String input;
        private int position;

        BitwiseExpressionParser(String input) {
            this.input = input.replaceAll("\\s+", "");
            this.position = 0;
        }

        int parse() {
            int result = parseBitwiseOr();

            if (position != input.length()) {
                throw new IllegalArgumentException("Unexpected input");
            }

            return result;
        }

        private int parseBitwiseOr() {
            int value = parseBitwiseXor();

            while (match("|")) {
                value |= parseBitwiseXor();
            }

            return value;
        }

        private int parseBitwiseXor() {
            int value = parseBitwiseAnd();

            while (match("^")) {
                value ^= parseBitwiseAnd();
            }

            return value;
        }

        private int parseBitwiseAnd() {
            int value = parseShift();

            while (match("&")) {
                value &= parseShift();
            }

            return value;
        }

        private int parseShift() {
            int value = parseUnary();

            while (true) {
                if (match("<<")) {
                    int shiftAmount = parseUnary();
                    validateShiftAmount(shiftAmount);
                    value <<= shiftAmount;
                } else if (match(">>")) {
                    int shiftAmount = parseUnary();
                    validateShiftAmount(shiftAmount);
                    value >>= shiftAmount;
                } else {
                    return value;
                }
            }
        }

        private int parseUnary() {
            if (match("~")) {
                return ~parseUnary();
            }

            return parsePrimary();
        }

        private int parsePrimary() {
            if (match("(")) {
                int value = parseBitwiseOr();

                if (!match(")")) {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }

                return value;
            }

            return parseNumber();
        }

        private int parseNumber() {
            if (position >= input.length() || !Character.isDigit(input.charAt(position))) {
                throw new IllegalArgumentException("Expected number");
            }

            long value = 0;

            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                value = value * 10 + (input.charAt(position) - '0');

                if (value > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Number is too large");
                }

                position++;
            }

            return (int) value;
        }

        private boolean match(String token) {
            if (input.startsWith(token, position)) {
                position += token.length();
                return true;
            }

            return false;
        }

        private void validateShiftAmount(int shiftAmount) {
            if (shiftAmount < 0 || shiftAmount > 31) {
                throw new IllegalArgumentException("Invalid shift amount");
            }
        }
    }
}