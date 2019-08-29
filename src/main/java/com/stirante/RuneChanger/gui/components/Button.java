package com.stirante.RuneChanger.gui.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Button extends Component {
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_HOVERED = 1;
    private static final int STATE_PRESSED = 2;
    private static final int STATE_DISABLED = 3;

    private StringProperty textProperty = new SimpleStringProperty();
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
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

    private Image[] side = new Image[4];
    private Image[] center = new Image[4];

    public Button() {
        super();
        //Invalidate currently rendered element on change
        textProperty().addListener(this::invalidated);

        side[STATE_DEFAULT] = new Image(getClass().getResource("/images/leftstraightdefault.png").toExternalForm());
        side[STATE_HOVERED] = new Image(getClass().getResource("/images/leftstraighthover.png").toExternalForm());
        side[STATE_PRESSED] = new Image(getClass().getResource("/images/leftstraightclick.png").toExternalForm());
        side[STATE_DISABLED] = new Image(getClass().getResource("/images/leftstraightdisabled.png").toExternalForm());

        center[STATE_DEFAULT] = new Image(getClass().getResource("/images/middefault.png").toExternalForm());
        center[STATE_HOVERED] = new Image(getClass().getResource("/images/midhover.png").toExternalForm());
        center[STATE_PRESSED] = new Image(getClass().getResource("/images/midclick.png").toExternalForm());
        center[STATE_DISABLED] = new Image(getClass().getResource("/images/middisabled.png").toExternalForm());
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
        onActionProperty().set(value);
    }

    public final EventHandler<ActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    @Override
    public void render(GraphicsContext g) {
        int currentState = disabledProperty().get() ? STATE_DISABLED :
                hoverProperty().get() ? STATE_HOVERED :
                        pressedProperty().get() ? STATE_PRESSED :
                                STATE_DEFAULT;
        g.clearRect(0, 0, getWidth(), getHeight());
        Image sideImage = side[currentState];
        Image centerImage = center[currentState];
        double sideWidth = (sideImage.getWidth() / sideImage.getHeight()) * getWidth();
        g.drawImage(sideImage, 0, 0, sideWidth, getHeight());
        g.drawImage(centerImage, sideWidth, 0, getWidth() - (2 * sideWidth), getHeight());
        g.drawImage(sideImage, getWidth() - sideWidth, 0, sideWidth, getHeight());
    }

    public StringProperty textProperty() {
        return textProperty;
    }

    public void setText(String textProperty) {
        this.textProperty.setValue(textProperty);
    }

    public String getText() {
        return textProperty.get();
    }
}
