package com.stirante.runechanger.gui.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class Button extends Component {
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_HOVERED = 1;
    private static final int STATE_PRESSED = 2;
    private static final int STATE_DISABLED = 3;
    private static final Color GOLD_COLOR = new Color(0xC8 / 255D, 0xAA / 255D, 0x6E / 255D, 1D);
    private static final Font FONT =
            Font.loadFont(Button.class.getResource("/fonts/Beaufort-Bold.ttf").toExternalForm(), 12);

    private StringProperty textProperty = new SimpleStringProperty();
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return Button.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };

    // --- tooltip
    private ObjectProperty<Tooltip> tooltip;
    private Image[] side = new Image[4];
    private Image[] center = new Image[4];

    public Button() {
        super();
        //Invalidate currently rendered element on change
        textProperty().addListener(this::invalidated);
        hoverProperty().addListener(this::invalidated);
        pressedProperty().addListener(this::invalidated);
        onMouseClickedProperty().setValue(event -> {
            if (onAction.get() != null) {
                onAction.get().handle(new ActionEvent(this, this));
            }
        });

        side[STATE_DEFAULT] = new Image(getClass().getResource("/images/leftstraightdefault.png").toExternalForm());
        side[STATE_HOVERED] = new Image(getClass().getResource("/images/leftstraighthover.png").toExternalForm());
        side[STATE_PRESSED] = new Image(getClass().getResource("/images/leftstraightclick.png").toExternalForm());
        side[STATE_DISABLED] = new Image(getClass().getResource("/images/leftstraightdisabled.png").toExternalForm());

        center[STATE_DEFAULT] = new Image(getClass().getResource("/images/middefault.png").toExternalForm());
        center[STATE_HOVERED] = new Image(getClass().getResource("/images/midhover.png").toExternalForm());
        center[STATE_PRESSED] = new Image(getClass().getResource("/images/midclick.png").toExternalForm());
        center[STATE_DISABLED] = new Image(getClass().getResource("/images/middisabled.png").toExternalForm());
    }

    /**
     * The ToolTip for this control.
     *
     * @return the tool tip for this control
     */
    public final ObjectProperty<Tooltip> tooltipProperty() {
        if (tooltip == null) {
            tooltip = new ObjectPropertyBase<>() {
                private Tooltip old = null;

                @Override
                protected void invalidated() {
                    Tooltip t = get();
                    // install / uninstall
                    if (t != old) {
                        if (old != null) {
                            Tooltip.uninstall(Button.this, old);
                        }
                        if (t != null) {
                            Tooltip.install(Button.this, t);
                        }
                        old = t;
                    }
                }

                @Override
                public Object getBean() {
                    return Button.this;
                }

                @Override
                public String getName() {
                    return "tooltip";
                }
            };
        }
        return tooltip;
    }

    public final Tooltip getTooltip() {
        return tooltip == null ? null : tooltip.getValue();
    }

    public final void setTooltip(Tooltip value) {
        tooltipProperty().setValue(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    public final EventHandler<ActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
        onActionProperty().set(value);
    }

    @Override
    public void render(GraphicsContext g) {
        int currentState = disabledProperty().get() ? STATE_DISABLED :
                pressedProperty().get() ? STATE_PRESSED :
                        hoverProperty().get() ? STATE_HOVERED :
                                STATE_DEFAULT;
        g.clearRect(0, 0, getWidth(), getHeight());
        Image sideImage = side[currentState];
        Image centerImage = center[currentState];
        double sideWidth = (sideImage.getWidth() / sideImage.getHeight()) * getHeight();
        g.drawImage(sideImage, 0, 0, sideWidth, getHeight());
        g.drawImage(centerImage, sideWidth, 0, getWidth() - (2 * sideWidth), getHeight());
        g.drawImage(sideImage, getWidth(), 0, -sideWidth, getHeight());
        g.setFill(GOLD_COLOR);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(FONT);
        g.fillText(textProperty.get(), getWidth() / 2, getHeight() / 2);
    }

    public StringProperty textProperty() {
        return textProperty;
    }

    public String getText() {
        return textProperty.get();
    }

    public void setText(String textProperty) {
        this.textProperty.setValue(textProperty);
    }

}
