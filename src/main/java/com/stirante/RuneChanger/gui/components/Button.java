package com.stirante.RuneChanger.gui.components;

import javafx.beans.NamedArg;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.canvas.GraphicsContext;

public class Button extends Component {
    private StringProperty textProperty = new SimpleStringProperty();

    public Button(@NamedArg("text") String text) {
        super();
        //Invalidate currently rendered element on change
        textProperty().addListener(this::invalidated);

        //Set initial text
        textProperty().setValue(text);
    }

    @Override
    public void render(GraphicsContext g) {
        g.clearRect(0, 0, getWidth(), getHeight());
        //TODO
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
