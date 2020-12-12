package hidemod;

import java.util.List;

import helper.RayTracer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class HideGunCommand extends CommandBase {

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getName() {
		return "hide";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args[0].equalsIgnoreCase("debug")) {
			RayTracer.debug = !RayTracer.debug;
			sender.sendMessage(new TextComponentString("now debug rect = " + RayTracer.debug));
			return;
		} else if (args[0].equalsIgnoreCase("reload")) {
			notifyCommandListener(sender, this, 1, "commands.hide.reload.success", new Object[] {});

			return;
		}
		RayTracer.Comp = Float.valueOf(args[0]);
		sender.sendMessage(new TextComponentString("now Comp tick = " + RayTracer.Comp));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos targetPos) {
		return super.getTabCompletions(server, sender, args, targetPos);
	}
}