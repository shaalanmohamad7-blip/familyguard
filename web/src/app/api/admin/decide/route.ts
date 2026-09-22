import { NextResponse } from "next/server";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { isAdminSession } from "@/lib/admin";

export const dynamic = "force-dynamic";

const schema = z.object({
  userId: z.string().min(1),
  decision: z.enum(["approve", "reject"]),
});

// Approves or rejects a pending account. Owner-only.
export async function POST(req: Request) {
  if (!(await isAdminSession())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const json = await req.json().catch(() => null);
  const parsed = schema.safeParse(json);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input" }, { status: 400 });
  }

  const { userId, decision } = parsed.data;
  const status = decision === "approve" ? "ACTIVE" : "REJECTED";

  const user = await prisma.user.update({
    where: { id: userId },
    data: { status },
    select: { id: true, status: true },
  });

  return NextResponse.json({ ok: true, user });
}
