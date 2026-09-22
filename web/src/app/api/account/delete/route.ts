import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireParentContext } from "@/lib/parentSession";
import { logAccess } from "@/lib/accessLog";

export const dynamic = "force-dynamic";

// Permanently and immediately deletes every row scoped to the caller's
// family: devices, their locations/usage/limits, geofences, pairing codes,
// alerts, access logs, memberships, the family itself, and any user whose
// only membership was this family. There is no soft-delete or grace period.
export async function POST() {
  const ctx = await requireParentContext();
  if (!ctx) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const familyId = ctx.familyId;

  await logAccess({
    familyId,
    actorType: "USER",
    actorId: ctx.userId,
    action: "FAMILY_DATA_DELETE_REQUESTED",
    targetType: "Family",
    targetId: familyId,
  });

  const devices = await prisma.device.findMany({
    where: { familyId },
    select: { id: true },
  });
  const deviceIds = devices.map((d) => d.id);
  const memberships = await prisma.familyMember.findMany({
    where: { familyId },
    select: { userId: true },
  });
  const memberUserIds = memberships.map((m) => m.userId);

  await prisma.$transaction([
    prisma.locationPoint.deleteMany({ where: { deviceId: { in: deviceIds } } }),
    prisma.usageRecord.deleteMany({ where: { deviceId: { in: deviceIds } } }),
    prisma.limitRule.deleteMany({ where: { deviceId: { in: deviceIds } } }),
    prisma.device.deleteMany({ where: { familyId } }),
    prisma.pairingCode.deleteMany({ where: { familyId } }),
    prisma.geofence.deleteMany({ where: { familyId } }),
    prisma.alert.deleteMany({ where: { familyId } }),
    prisma.accessLog.deleteMany({ where: { familyId } }),
    prisma.familyMember.deleteMany({ where: { familyId } }),
    prisma.family.delete({ where: { id: familyId } }),
  ]);

  // Remove any user account that no longer belongs to any family.
  for (const userId of memberUserIds) {
    const remaining = await prisma.familyMember.count({ where: { userId } });
    if (remaining === 0) {
      await prisma.user.delete({ where: { id: userId } }).catch(() => undefined);
    }
  }

  return NextResponse.json({ ok: true });
}
