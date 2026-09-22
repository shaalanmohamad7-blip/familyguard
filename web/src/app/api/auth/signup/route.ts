import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { logAccess } from "@/lib/accessLog";
import { isAdminEmail } from "@/lib/admin";

export const dynamic = "force-dynamic";

const signupSchema = z.object({
  name: z.string().min(1).max(200),
  familyName: z.string().min(1).max(200),
  email: z.string().email().max(320),
  password: z.string().min(8).max(200),
});

export async function POST(req: Request) {
  const json = await req.json().catch(() => null);
  const parsed = signupSchema.safeParse(json);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input" }, { status: 400 });
  }

  const { name, familyName, email, password } = parsed.data;
  const normalizedEmail = email.toLowerCase().trim();

  const existing = await prisma.user.findUnique({ where: { email: normalizedEmail } });
  if (existing) {
    return NextResponse.json({ error: "Email already in use" }, { status: 409 });
  }

  const passwordHash = await bcrypt.hash(password, 10);

  // The platform owner (ADMIN_EMAIL) is auto-activated; everyone else waits
  // for the owner to approve them from the admin panel.
  const status = isAdminEmail(normalizedEmail) ? "ACTIVE" : "PENDING";

  const result = await prisma.$transaction(async (tx) => {
    const user = await tx.user.create({
      data: { email: normalizedEmail, passwordHash, name, status },
    });
    const family = await tx.family.create({
      data: { name: familyName, isDemo: false },
    });
    await tx.familyMember.create({
      data: { userId: user.id, familyId: family.id, role: "PARENT" },
    });
    return { user, family };
  });

  await logAccess({
    familyId: result.family.id,
    actorType: "USER",
    actorId: result.user.id,
    action: "SIGNUP",
    targetType: "Family",
    targetId: result.family.id,
  });

  return NextResponse.json(
    { id: result.user.id, status },
    { status: 201 }
  );
}
