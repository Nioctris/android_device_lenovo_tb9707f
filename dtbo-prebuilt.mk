# Install the stock ZUI dtbo (13 overlays: panel, touch, audio, charger...)
# instead of building one from the kernel DTBO output.
# BOARD_PREBUILT_DTBOIMAGE must live under OUT_DIR; the source
# tree is read-only for Kati.
$(BOARD_PREBUILT_DTBOIMAGE): $(DEVICE_PATH)/prebuilt/dtbo.img
	@mkdir -p $(dir $@)
	cp $< $@
