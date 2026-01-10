/**
 * Created by Elec332 on 23-04-2024
 * Refactored by ErenayDev
 */

package nl.elec332.minecraft.singleplayerserversettings.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ShareToLanScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ShareToLanScreen.class)
public abstract class ShareToLanScreenMixin extends Screen {

    @Unique
    private boolean sps$onlineMode;

    @Unique
    private boolean sps$pvpAllowed;

    @Unique
    private String sps$motd;

    @Unique
    private int sps$port = 25565;

    @Unique
    private GameType sps$gameMode = GameType.SURVIVAL;

    @Unique
    private boolean sps$allowCheats = false;

    @Unique
    private TextFieldWidget sps$portField;

    @Unique
    private TextFieldWidget sps$motdField;

    @Unique
    private static final ITextComponent OFFLINE_MODE_ON = new StringTextComponent("Offline Mode: ON");

    @Unique
    private static final ITextComponent OFFLINE_MODE_OFF = new StringTextComponent("Offline Mode: OFF");

    @Unique
    private static final ITextComponent PVP_ON = new StringTextComponent("PvP: ON");

    @Unique
    private static final ITextComponent PVP_OFF = new StringTextComponent("PvP: OFF");

    @Unique
    private static final ITextComponent CHEATS_ON = new StringTextComponent("Allow Cheats: ON");

    @Unique
    private static final ITextComponent CHEATS_OFF = new StringTextComponent("Allow Cheats: OFF");

    @Unique
    private static final ITextComponent PORT_LABEL = new TranslationTextComponent("lanServer.port");

    @Unique
    private static final ITextComponent MOTD_LABEL = new StringTextComponent("MOTD");

    protected ShareToLanScreenMixin(ITextComponent title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void sps$onInit(CallbackInfo ci) {
        ci.cancel();

        IntegratedServer server = Objects.requireNonNull(Objects.requireNonNull(this.minecraft).getSingleplayerServer());

        this.sps$onlineMode = server.usesAuthentication();
        this.sps$pvpAllowed = server.isPvpAllowed();
        this.sps$motd = server.getMotd();
        this.sps$gameMode = server.getDefaultGameType();

        this.addButton(new Button(
                this.width / 2 - 155, 50, 150, 20,
                new StringTextComponent("Game Mode: " + this.sps$gameMode.getName()),
                button -> {
                    this.sps$gameMode = GameType.byId((this.sps$gameMode.getId() + 1) % 4);
                    button.setMessage(new StringTextComponent("Game Mode: " + this.sps$gameMode.getName()));
                }
        ));

        this.addButton(new Button(
                this.width / 2 + 5, 50, 150, 20,
                this.sps$allowCheats ? CHEATS_ON : CHEATS_OFF,
                button -> {
                    this.sps$allowCheats = !this.sps$allowCheats;
                    button.setMessage(this.sps$allowCheats ? CHEATS_ON : CHEATS_OFF);
                }
        ));

        this.addButton(new Button(
                this.width / 2 - 155, 80, 150, 20,
                this.sps$onlineMode ? OFFLINE_MODE_OFF : OFFLINE_MODE_ON,
                button -> {
                    this.sps$onlineMode = !this.sps$onlineMode;
                    button.setMessage(this.sps$onlineMode ? OFFLINE_MODE_OFF : OFFLINE_MODE_ON);
                }
        ));

        this.addButton(new Button(
                this.width / 2 + 5, 80, 150, 20,
                this.sps$pvpAllowed ? PVP_ON : PVP_OFF,
                button -> {
                    this.sps$pvpAllowed = !this.sps$pvpAllowed;
                    button.setMessage(this.sps$pvpAllowed ? PVP_ON : PVP_OFF);
                }
        ));

        this.sps$portField = new TextFieldWidget(this.font, this.width / 2 - 155, 125, 150, 20, PORT_LABEL);
        this.sps$portField.setValue(String.valueOf(this.sps$port));
        this.sps$portField.setResponder(s -> {
            try {
                int port = Integer.parseInt(s);
                if (port > 0 && port <= 65535) {
                    this.sps$port = port;
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.children.add(this.sps$portField);

        this.sps$motdField = new TextFieldWidget(this.font, this.width / 2 + 5, 125, 150, 20, MOTD_LABEL);
        this.sps$motdField.setMaxLength(64);
        this.sps$motdField.setValue(this.sps$motd);
        this.sps$motdField.setResponder(s -> this.sps$motd = s);
        this.children.add(this.sps$motdField);

        this.addButton(new Button(
                this.width / 2 - 155, 170, 150, 20,
                new TranslationTextComponent("lanServer.start"),
                button -> this.sps$startLanServer()
        ));

        this.addButton(new Button(
                this.width / 2 + 5, 170, 150, 20,
                new TranslationTextComponent("gui.cancel"),
                button -> this.minecraft.setScreen(null)
        ));
    }

    @Unique
    private void sps$startLanServer() {
        IntegratedServer server = Objects.requireNonNull(Objects.requireNonNull(this.minecraft).getSingleplayerServer());

        server.setUsesAuthentication(this.sps$onlineMode);
        server.setPvpAllowed(this.sps$pvpAllowed);
        server.setMotd(this.sps$motd);

        boolean success = server.publishServer(this.sps$gameMode, this.sps$allowCheats, this.sps$port);

        ITextComponent message;
        if (success) {
            message = new TranslationTextComponent("commands.publish.started", this.sps$port);
            this.minecraft.gui.getChat().addMessage(
                    new StringTextComponent("Offline Mode: " + !this.sps$onlineMode + " | PvP: " + this.sps$pvpAllowed + " | MOTD: " + this.sps$motd)
            );
        } else {
            message = new TranslationTextComponent("commands.publish.failed");
        }

        this.minecraft.gui.getChat().addMessage(message);
        this.minecraft.setScreen(null);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sps$onRender(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ci.cancel();

        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        drawString(matrixStack, this.font, PORT_LABEL, this.width / 2 - 155, 113, 0xFFFFFF);
        drawString(matrixStack, this.font, MOTD_LABEL, this.width / 2 + 5, 113, 0xFFFFFF);

        if (this.sps$portField != null) {
            this.sps$portField.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        if (this.sps$motdField != null) {
            this.sps$motdField.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

}
