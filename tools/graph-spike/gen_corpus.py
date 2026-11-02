#!/usr/bin/env python3
"""Generate a controlled press-release / official-record corpus with a planted answer.

The planted question is: supplier "Kestrel Memory Works" claims it supplies memory
to "four of the world's five top GPU producers". Which four, and which one is out?

No single document states the pairing set. The answer requires:
  (a) the aggregate cardinality claim (4 of 5), and
  (b) scattered, unevenly strong per-entity evidence, and
  (c) resistance to decoy documents that co-mention two companies without
      asserting any supply relation.

Ground truth is written to truth/ground_truth.json, which no arm reads.

Usage:
  python3 gen_corpus.py --out corpus --truth truth --seed 20261029
  python3 gen_corpus.py --out corpus_noagg --truth truth --seed 20261029 --no-aggregate
"""

import argparse
import glob
import json
import os
import random
import textwrap  # noqa: F401  (kept: deindent replaced its use)

# --------------------------------------------------------------------------
# Entities. Everything is fictional on purpose.
# --------------------------------------------------------------------------

TOP5_GPU = [
    "Tessara Graphics Corporation",
    "Nyxvolt Systems Inc.",
    "Orbitine Compute AB",
    "Halcyon Microsystems",
    "Veradyne Silicon N.V.",
]

# GPU producers that exist but are outside the top five, so "top five" bites.
OTHER_GPU = ["Brightfell Graphics", "Cindermere Visual Systems"]

SUPPLIER_A = "Kestrel Memory Works"
OTHER_SUPPLIERS = ["Lumenstor Memory", "Sable Dynamics", "Arcwell Semiconductor"]

# Kestrel customers that are not GPU producers at all. A naive "count Kestrel's
# supply edges" answers 7, not 4, unless the query filters on entity type.
NON_GPU_CUSTOMERS = [
    "Cobalt Ridge Networking",
    "Peregrine Automotive Electronics",
    "Thornbury Aerospace Systems",
]

# Near-duplicate names. Entity resolution has to go both ways:
#   - Nyxvolt Labs must NOT be merged into Nyxvolt Systems Inc. (over-merge trap)
#   - "Halcyon Micro" MUST be merged into Halcyon Microsystems (under-merge trap)
#   - Kestrel Analytics Group must NOT be merged into Kestrel Memory Works
NEAR_DUPES = {
    "Nyxvolt Labs": "unrelated AI software firm, not the GPU producer",
    "Halcyon Micro": "informal alias of Halcyon Microsystems, same company",
    "Kestrel Analytics Group": "market research firm, unrelated to Kestrel Memory Works",
}

# The planted answer.
SUPPLIED = [
    "Tessara Graphics Corporation",
    "Nyxvolt Systems Inc.",
    "Orbitine Compute AB",
    "Halcyon Microsystems",
]
EXCLUDED = "Veradyne Silicon N.V."

BOILER = [
    "This release contains forward-looking statements that involve risks and "
    "uncertainties. Actual results may differ materially from those expressed or "
    "implied. The company undertakes no obligation to update any forward-looking "
    "statement, whether as a result of new information, future events or otherwise.",
    "Figures presented in this document are unaudited and prepared on a management "
    "basis. Comparatives have been restated where segment definitions changed. A "
    "reconciliation of non-statutory measures to the nearest statutory equivalent is "
    "available in the investor relations section of the corporate website.",
    "Media enquiries should be directed to the corporate communications desk. "
    "Investor enquiries should be directed to investor relations. Product names and "
    "trademarks referenced in this document are the property of their respective "
    "owners and are used for identification purposes only.",
    "The company operates design, validation and customer engineering sites across "
    "three regions and employs staff in a majority of its engineering functions on a "
    "hybrid basis. Supplier arrangements are reviewed annually under the company's "
    "procurement governance framework.",
    "No part of this announcement constitutes an offer to sell or a solicitation of "
    "an offer to buy any security. Distribution of this announcement in certain "
    "jurisdictions may be restricted by law, and persons into whose possession it "
    "comes should inform themselves about and observe any such restriction.",
    "Environmental, social and governance disclosures are published annually and "
    "assured to a limited level by an independent practitioner. Scope 3 categories "
    "remain subject to estimation uncertainty, particularly for purchased goods and "
    "services within the semiconductor supply base.",
]

ABOUT = {
    "Tessara Graphics Corporation": "Tessara Graphics Corporation designs discrete "
    "graphics processors and accelerators for gaming, visualisation and datacentre "
    "workloads. Its current product families are built on the Lanthorn architecture.",
    "Nyxvolt Systems Inc.": "Nyxvolt Systems Inc. builds graphics and accelerator "
    "silicon for workstation, edge and datacentre deployment. Its accelerator line is "
    "marketed under the Aurora platform name.",
    "Orbitine Compute AB": "Orbitine Compute AB is a graphics and compute silicon "
    "designer headquartered in northern Europe, serving scientific computing, "
    "automotive visualisation and cloud rendering customers.",
    "Halcyon Microsystems": "Halcyon Microsystems develops graphics processors and "
    "co-packaged accelerator modules. The company is also referred to in trade press "
    "and in some supplier documentation as Halcyon Micro.",
    "Veradyne Silicon N.V.": "Veradyne Silicon N.V. designs discrete GPUs and "
    "accelerator boards, with its principal product line sold as the H-series.",
    "Brightfell Graphics": "Brightfell Graphics supplies entry-level and embedded "
    "graphics silicon, primarily to industrial and point-of-sale integrators.",
    "Cindermere Visual Systems": "Cindermere Visual Systems produces graphics "
    "subsystems for broadcast, simulation and medical imaging markets.",
    SUPPLIER_A: "Kestrel Memory Works manufactures high-bandwidth and low-power "
    "memory, including the KM-9 HBM3E stack family, for compute, networking, "
    "automotive and aerospace customers.",
    "Lumenstor Memory": "Lumenstor Memory produces graphics and high-bandwidth memory "
    "for accelerator and console applications.",
    "Sable Dynamics": "Sable Dynamics manufactures DRAM and high-bandwidth memory "
    "products, together with memory controller IP.",
    "Arcwell Semiconductor": "Arcwell Semiconductor supplies graphics memory, "
    "high-bandwidth memory and non-volatile memory to compute customers worldwide.",
    "Nyxvolt Labs": "Nyxvolt Labs is an independent artificial intelligence software "
    "research company. It is not affiliated with, and has no ownership relationship "
    "to, Nyxvolt Systems Inc.",
    "Kestrel Analytics Group": "Kestrel Analytics Group is an independent technology "
    "market research firm. It has no corporate relationship with Kestrel Memory Works.",
}


def pad(body, rng, n):
    """Append n boilerplate blocks so document length varies."""
    picks = rng.sample(BOILER, min(n, len(BOILER)))
    return body + ("\n\n" + "\n\n".join(picks) if picks else "")


def about_block(names):
    return "\n\n".join("About %s. %s" % (n, ABOUT[n]) for n in names if n in ABOUT)


DOCS = []


def deindent(text):
    """Strip the triple-quote indentation while keeping relative indentation.

    textwrap.dedent cannot do this here: the bodies are built by concatenating
    an indented literal with unindented blocks, so the common prefix is "" and
    dedent becomes a no-op, leaving the corpus full of ragged leading spaces.
    """
    lines = text.replace("\t", "    ").split("\n")
    ref = 0
    for l in lines:
        if l.strip():
            ref = len(l) - len(l.lstrip(" "))
            break
    if not ref:
        return text.strip()
    out = []
    for l in lines:
        out.append(l[ref:] if l[:ref].strip() == "" else l.lstrip(" "))
    return "\n".join(out).strip()


def doc(did, title, date, source, dtype, body, tags=()):
    DOCS.append(
        {
            "id": did,
            "title": title,
            "date": date,
            "source": source,
            "type": dtype,
            "text": deindent(body),
            "_tags": list(tags),
        }
    )


NUMWORD = {2: "two", 3: "three", 4: "four", 5: "five"}


def build(rng, include_aggregate=True, two_zero=False, claim_n=4,
          veradyne_evidence=False):
    """Perturbations, so the follow-up variants stay comparable with the primary.

    two_zero          -- remove Halcyon's two evidence links, so TWO producers
                         have zero extracted evidence and absence alone cannot
                         pick the excluded one.
    claim_n           -- the N in the "N of five" aggregate claim.
    veradyne_evidence -- plant a genuine joint supply release for Veradyne, so
                         all five producers carry evidence and the claim of
                         four becomes unsatisfiable.
    """
    DOCS.clear()

    # ---------------- the aggregate cardinality claim ----------------
    if include_aggregate:
        doc(
            "KMW-2026-014",
            "Kestrel Memory Works reports record high-bandwidth memory shipments",
            "2026-07-21",
            "Kestrel Memory Works newsroom",
            "press_release",
            """
            Kestrel Memory Works today reported record quarterly shipments of
            high-bandwidth memory, driven by demand for its KM-9 HBM3E stack family.

            "High-bandwidth memory is now the gating component for accelerator
            roadmaps, and our customers plan around our capacity two years out," said
            Imogen Rask, chief executive. "Kestrel supplies high-bandwidth memory to
            %s of the world's five top GPU producers, and we intend to keep that
            position through the HBM4 transition."

            The company did not disclose customer names, citing confidentiality
            obligations under its supply agreements. Kestrel said capacity additions at
            its stacking and test facility would come online through the next fiscal
            year.
            """ % NUMWORD[claim_n]
            + "\n\n"
            + about_block([SUPPLIER_A]),
            tags=["aggregate_claim"],
        )

    # ---------------- the universe: who the top five are ----------------
    doc(
        "KAG-RANK-2026",
        "Kestrel Analytics Group: 2026 discrete GPU producer ranking",
        "2026-02-11",
        "Kestrel Analytics Group research note",
        "analyst_record",
        """
        Kestrel Analytics Group has published its 2026 ranking of discrete GPU
        producers by trailing twelve-month graphics and accelerator silicon revenue.

        The world's five top GPU producers, in rank order, are:
          1. Tessara Graphics Corporation
          2. Nyxvolt Systems Inc.
          3. Orbitine Compute AB
          4. Halcyon Microsystems
          5. Veradyne Silicon N.V.

        Ranked sixth and seventh respectively were Brightfell Graphics and Cindermere
        Visual Systems, neither of which reached the revenue threshold for inclusion in
        the top five. No other producer exceeded two per cent unit share.

        Kestrel Analytics Group is a market research firm and has no corporate
        relationship with the memory manufacturer Kestrel Memory Works, despite the
        shared first element of the two company names.
        """
        + "\n\n"
        + about_block(["Kestrel Analytics Group"]),
        tags=["universe"],
    )

    # ---------------- E1: Tessara. Explicit joint release. ----------------
    doc(
        "TSG-2026-031",
        "Tessara Graphics and Kestrel Memory Works sign multi-year HBM3E supply agreement",
        "2026-03-04",
        "Tessara Graphics Corporation newsroom",
        "joint_press_release",
        """
        Tessara Graphics Corporation and Kestrel Memory Works today announced a
        multi-year agreement under which Kestrel Memory Works will supply KM-9 HBM3E
        memory stacks for Tessara's Lanthorn-architecture datacentre accelerators.

        Under the agreement Kestrel becomes a qualified memory supplier to Tessara for
        the Lanthorn generation, with deliveries beginning this fiscal year and running
        through the subsequent product cycle. Tessara said the arrangement is
        non-exclusive and that it continues to source graphics memory from other
        vendors.

        "Securing stacked memory capacity early is now a design decision, not a
        procurement decision," said Dr Pell Anwar, senior vice president of silicon
        engineering at Tessara Graphics.
        """
        + "\n\n"
        + about_block(["Tessara Graphics Corporation", SUPPLIER_A]),
        tags=["evidence:Tessara:explicit"],
    )

    # ------ UNSAT variant only: genuine supply evidence for Veradyne too, so all
    # ------ five producers carry evidence and a claim of four cannot hold.
    if veradyne_evidence:
        doc(
            "VRD-2026-077",
            "Veradyne Silicon and Kestrel Memory Works announce HBM3E supply agreement",
            "2026-02-26",
            "Veradyne Silicon N.V. newsroom",
            "joint_press_release",
            """
            Veradyne Silicon N.V. and Kestrel Memory Works today announced an
            agreement under which Kestrel Memory Works will supply KM-9 HBM3E memory
            stacks for Veradyne's H-series accelerator boards.

            Kestrel becomes a qualified memory supplier to Veradyne for the H-series
            generation, with deliveries beginning this fiscal year. The companies said
            the arrangement is unaffected by their continuing patent dispute.
            """
            + "\n\n"
            + about_block(["Veradyne Silicon N.V.", SUPPLIER_A]),
            tags=["evidence:Veradyne:explicit_UNSAT_VARIANT"],
        )

    # ---------------- E2: Orbitine. Official record, buried in a long filing. --------
    doc(
        "ORB-SCD-2026",
        "Orbitine Compute AB statutory supply chain disclosure 2026",
        "2026-05-30",
        "Orbitine Compute AB regulatory filings",
        "official_record",
        """
        This disclosure is published by Orbitine Compute AB in accordance with its
        statutory supply chain transparency obligations and covers the financial year
        ended 31 December 2025.

        Section 1. Scope and governance. Orbitine Compute AB operates a tiered supplier
        classification. Tier 1 suppliers are those from whom the company procures
        production material directly and with whom it holds a master supply agreement.
        All tier 1 suppliers are subject to annual on-site audit and to the company's
        responsible sourcing code.

        Section 2. Tier 1 production material suppliers of record. For the reporting
        period, Orbitine Compute AB's tier 1 suppliers of record for production
        material were as follows:

          - Kestrel Memory Works (high-bandwidth memory components)
          - Lumenstor Memory (graphics memory components)
          - Calderway Substrates (organic substrate)
          - Pellingham Test Services (final test and burn-in)
          - Vorsa Thermal (thermal solutions)

        Each of the above entities has provided a signed declaration of compliance with
        the responsible sourcing code. No tier 1 supplier was suspended during the
        period.

        Section 3. Conflict minerals. The company conducted reasonable country of
        origin enquiry across its tier 1 base. No smelter of concern was identified in
        the tantalum or tungsten streams. Two tin smelters remained outside the
        conformant list at period end and are subject to a corrective action plan.

        Section 4. Labour standards. Audits were completed at eleven supplier sites.
        Four non-conformities were raised, all classified minor, relating to working
        hours records and to the storage of personal protective equipment. All four
        were closed within the period.

        Section 5. Grievance mechanism. The company operates an externally hosted
        grievance line available in six languages. Seven reports were received during
        the period, of which none were substantiated as relating to labour standards in
        the production material supply base.

        Section 6. Climate related supplier engagement. Orbitine engaged its ten
        largest suppliers by spend on scope 1 and scope 2 reduction targets. Six have
        published targets validated by an external body. Engagement continues with the
        remainder.

        Section 7. Approval. This disclosure was approved by the board of Orbitine
        Compute AB and signed on its behalf by the chief operating officer.
        """
        + "\n\n"
        + about_block(["Orbitine Compute AB"]),
        tags=["evidence:Orbitine:official_record"],
    )

    # ---------------- E3: Nyxvolt Systems. Three-document chain, never joint. -------
    doc(
        "KMW-2026-009",
        "Kestrel KM-9 HBM3E stack completes qualification on tier-one Aurora-class platform",
        "2026-04-18",
        "Kestrel Memory Works newsroom",
        "qualification",
        """
        Kestrel Memory Works announced today that its KM-9 HBM3E memory stack has
        completed full production qualification on a tier-one customer's Aurora-class
        accelerator platform, clearing signal integrity, thermal cycling and extended
        burn-in gates.

        The customer was not named. Kestrel said qualification covered the 12-high
        stack configuration at the platform's full rated bandwidth, and that volume
        deliveries against the programme would begin in the current quarter.
        """
        + "\n\n"
        + about_block([SUPPLIER_A]),
        tags=["evidence:Nyxvolt:hint_qual"],
    )
    doc(
        "NVS-2026-022",
        "Nyxvolt Systems launches Aurora accelerator platform",
        "2026-01-29",
        "Nyxvolt Systems Inc. newsroom",
        "product_launch",
        """
        Nyxvolt Systems Inc. today launched Aurora, its new accelerator platform for
        datacentre training and inference workloads. Aurora parts use stacked
        high-bandwidth memory in a 12-high configuration on a silicon interposer.

        Nyxvolt Systems did not disclose its memory suppliers for the platform. The
        company said Aurora samples are with lead customers and that general
        availability is expected within the year.
        """
        + "\n\n"
        + about_block(["Nyxvolt Systems Inc."]),
        tags=["evidence:Nyxvolt:hint_platform"],
    )
    doc(
        "NVS-JOB-4471",
        "Nyxvolt Systems vacancy: Senior Memory Integration Engineer, Aurora programme",
        "2026-06-02",
        "Nyxvolt Systems Inc. careers page",
        "job_advert",
        """
        Nyxvolt Systems Inc. is recruiting a Senior Memory Integration Engineer to join
        the Aurora platform silicon team.

        Responsibilities include stacked memory bring-up, training sequence debug and
        interposer signal integrity correlation on Aurora silicon.

        Essential experience:
          - Hands-on bring-up of Kestrel KM-9 HBM3E stacks in a production programme
          - PHY training and margining on 12-high stacked memory
          - Familiarity with vendor qualification gates and 8D failure analysis

        Desirable experience:
          - Prior work with interposer-level thermal correlation
          - Scripting in Python for automated margin sweeps

        This vacancy sits within the Aurora programme and reports to the memory
        subsystem lead.
        """,
        tags=["evidence:Nyxvolt:hint_job"],
    )

    # ---------------- E4: Halcyon. Weakest positive: alias + co-presentation. -------
    doc(
        "KMW-CALL-Q3",
        "Kestrel Memory Works third quarter earnings call transcript (edited extract)",
        "2026-08-06",
        "Kestrel Memory Works investor relations",
        "earnings_transcript",
        """
        Operator: Your next question comes from the line of a research analyst.

        Analyst: Can you talk about ramp linearity across the stacked memory programmes
        in the quarter?

        Imogen Rask, chief executive: Sure. Stacked memory revenue was up again
        sequentially. %s moved to volume during the quarter,
        which was the main sequential driver, and the networking programmes were flat.
        We shipped to plan on the large accelerator programmes.

        Analyst: And on mix?

        Chief financial officer: Mix was richer than last quarter. The co-packaged work
        carries a better margin than commodity graphics memory. We are not going to
        break out programme-level revenue.

        Analyst: Understood. On capacity.

        Imogen Rask: Capacity additions are on schedule. We are not capacity limited on
        test for this fiscal year. We will be tight on stacking in the first half of
        next year if the demand signal holds, and we have told our customers that.

        Operator: The next question comes from another analyst.

        Analyst: A quick one on customer concentration. Any change?

        Chief financial officer: No material change. Two customers were above ten per
        cent of revenue in the quarter. We disclose that annually and we are not going
        to name them.
        """ % ("The co-packaged programme" if two_zero
               else "The Halcyon Micro programme"),
        tags=["evidence:Halcyon:hint_alias"],
    )
    doc(
        "GMS-2026-AGENDA",
        "GPU Memory Summit 2026: conference programme",
        "2026-09-15",
        "GPU Memory Summit organising committee",
        "conference_programme",
        """
        The GPU Memory Summit 2026 technical programme is now published.

        Session 3A. Co-packaged memory at scale: lessons from a production ramp.
        %s The
        speakers will describe bring-up, yield learning and field data from a
        co-packaged stacked-memory product that shipped in volume this year.

        Session 3B. Interposer thermal gradients in 12-high stacks. Presented by
        Orbitine Compute AB.

        Session 4A. Graphics memory roadmap beyond GDDR7. Presented by Arcwell
        Semiconductor.

        Session 4B. Memory-aware scheduling for mixture-of-experts inference. Presented
        by Nyxvolt Labs.

        Session 5A. Test cost reduction in stacked memory. Presented by Sable Dynamics.

        Session 5B. Panel: HBM4 readiness. Panellists from Veradyne Silicon, Lumenstor
        Memory, Brightfell Graphics and the Open Memory Standards Council.
        """ % ("Presented by Kestrel Memory Works." if two_zero
               else "Jointly presented by Halcyon Microsystems and "
                    "Kestrel Memory Works."),
        tags=["evidence:Halcyon:hint_conference"],
    )

    # ---------------- Decoys around Veradyne. No supply relation asserted. ----------
    doc(
        "LIT-2026-0041",
        "Kestrel Memory Works files patent infringement action against Veradyne Silicon",
        "2026-03-19",
        "Court filings digest",
        "litigation",
        """
        Kestrel Memory Works has filed a patent infringement action against Veradyne
        Silicon N.V. concerning stacked-die interconnect structures used in
        high-bandwidth memory assemblies.

        The complaint alleges that Veradyne Silicon's H-series GPU products incorporate
        memory assemblies that practise three claims of a Kestrel patent covering
        through-silicon via placement. Kestrel seeks damages and injunctive relief.

        Veradyne Silicon said the claims are without merit and that it will defend the
        action vigorously. The dispute does not affect either company's current
        memory supply chain arrangements, a Veradyne spokesperson said.

        The two companies are among the most frequently cited parties in
        high-bandwidth memory patent families and have litigated against one another
        previously.
        """
        + "\n\n"
        + about_block(["Veradyne Silicon N.V.", SUPPLIER_A]),
        tags=["decoy:Veradyne:litigation"],
    )
    doc(
        "LIT-2026-0088",
        "Veradyne Silicon files counterclaim against Kestrel Memory Works",
        "2026-06-24",
        "Court filings digest",
        "litigation",
        """
        Veradyne Silicon N.V. has filed a counterclaim against Kestrel Memory Works in
        the stacked-die interconnect dispute, asserting two patents of its own and
        seeking discovery into Kestrel's stacking process.

        In the counterclaim Veradyne states that its memory procurement for the
        H-series programme is conducted under confidential terms and objects to
        Kestrel's discovery requests touching on Veradyne's memory sourcing decisions
        as disproportionate.

        Both parties have been ordered to attend mediation. Neither company would
        comment beyond its filings. Analysts noted that litigation between memory
        manufacturers and GPU producers is common and rarely disrupts commercial
        relationships.
        """
        + "\n\n"
        + about_block(["Veradyne Silicon N.V.", SUPPLIER_A]),
        tags=["decoy:Veradyne:litigation2"],
    )
    doc(
        "OMSC-2026-07",
        "Open Memory Standards Council ratifies HBM4 electrical profile",
        "2026-07-02",
        "Open Memory Standards Council",
        "standards_record",
        """
        The Open Memory Standards Council has ratified the HBM4 electrical profile
        following a two-year working group process.

        Working group 4, which produced the profile, comprised voting representatives
        from Kestrel Memory Works, Veradyne Silicon N.V., Arcwell Semiconductor,
        Tessara Graphics Corporation, Sable Dynamics and Lumenstor Memory.

        Membership of a working group reflects participation in standards development
        only. The Council notes, as it does in all ratification records, that working
        group membership does not indicate any commercial, supply or licensing
        relationship between member organisations.
        """,
        tags=["decoy:Veradyne:standards"],
    )
    doc(
        "SAE-2026-02",
        "Semiconductor Alliance of Europe appoints two new board members",
        "2026-04-09",
        "Semiconductor Alliance of Europe",
        "industry_body",
        """
        The Semiconductor Alliance of Europe has appointed representatives of Kestrel
        Memory Works and Veradyne Silicon N.V. to its board for a two-year term.

        The Alliance is a trade body representing design, manufacturing and equipment
        companies. Board membership carries no commercial obligation between member
        firms and does not imply any trading relationship.

        The outgoing board members, from Calderway Substrates and Brightfell Graphics,
        were thanked for their service.
        """,
        tags=["decoy:Veradyne:industry_body"],
    )
    # Over-merge trap: a Kestrel partnership with the near-duplicate Nyxvolt Labs.
    doc(
        "NXL-2026-005",
        "Nyxvolt Labs and Kestrel Memory Works collaborate on memory-aware inference scheduling",
        "2026-05-12",
        "Nyxvolt Labs newsroom",
        "partnership",
        """
        Nyxvolt Labs and Kestrel Memory Works today announced a research collaboration
        on memory-aware scheduling for large mixture-of-experts inference workloads.

        The collaboration is a software and modelling exercise. No hardware supply
        arrangement forms part of it. Kestrel will provide bandwidth and latency
        characterisation data for its KM-9 stacks; Nyxvolt Labs will publish an open
        source scheduler.

        Nyxvolt Labs is an independent artificial intelligence software research
        company and is not affiliated with Nyxvolt Systems Inc., the GPU producer with
        which it shares the first element of its name.
        """
        + "\n\n"
        + about_block(["Nyxvolt Labs", SUPPLIER_A]),
        tags=["decoy:over_merge_nyxvolt_labs"],
    )

    # -------- Competing supply relations: other suppliers to GPU producers. --------
    competing = [
        ("Arcwell Semiconductor", "Veradyne Silicon N.V.", "H-series graphics memory"),
        ("Lumenstor Memory", "Tessara Graphics Corporation", "GDDR7 graphics memory"),
        ("Sable Dynamics", "Halcyon Microsystems", "memory controller IP and DRAM"),
        ("Sable Dynamics", "Nyxvolt Systems Inc.", "commodity DRAM for board products"),
        ("Arcwell Semiconductor", "Brightfell Graphics", "embedded graphics memory"),
        ("Lumenstor Memory", "Cindermere Visual Systems", "graphics memory"),
    ]
    for i, (sup, cust, what) in enumerate(competing, start=1):
        doc(
            "SUP-%03d" % i,
            "%s to supply %s to %s" % (sup, what, cust),
            "2026-%02d-%02d" % (rng.randint(1, 10), rng.randint(1, 28)),
            "%s newsroom" % sup,
            "joint_press_release",
            """
            %s today announced an agreement to supply %s to %s.

            Deliveries begin this fiscal year. Neither company disclosed financial
            terms. %s said the arrangement is non-exclusive.
            """
            % (sup, what, cust, cust)
            + "\n\n"
            + about_block([sup, cust]),
            tags=["competing_supply"],
        )

    # -------- Kestrel supply to non-GPU customers: defeats naive edge counting. -----
    for i, cust in enumerate(NON_GPU_CUSTOMERS, start=1):
        doc(
            "KMW-NGC-%03d" % i,
            "Kestrel Memory Works named memory supplier to %s" % cust,
            "2026-%02d-%02d" % (rng.randint(1, 10), rng.randint(1, 28)),
            "Kestrel Memory Works newsroom",
            "joint_press_release",
            """
            Kestrel Memory Works will supply low-power and high-bandwidth memory to %s
            under a multi-year agreement announced today.

            %s said the award followed a competitive qualification process. The parts
            supplied are from the KM-7 and KM-9 families.
            """
            % (cust, cust)
            + "\n\n"
            + about_block([SUPPLIER_A]),
            tags=["kestrel_non_gpu"],
        )

    # -------- sibling statutory disclosures, so Orbitine's document SHAPE is not
    # -------- itself the giveaway. These lists are explicitly partial ("suppliers
    # -------- above a spend threshold"), so none of them is negative evidence:
    # -------- Kestrel's absence from them implies nothing. The elimination of
    # -------- Veradyne therefore still rests on the cardinality claim alone.
    sibling_filings = [
        ("Veradyne Silicon N.V.", ["Arcwell Semiconductor (graphics memory)",
                                   "Calderway Substrates (organic substrate)",
                                   "Vorsa Thermal (thermal solutions)"]),
        ("Nyxvolt Systems Inc.", ["Sable Dynamics (commodity DRAM)",
                                  "Calderway Substrates (organic substrate)",
                                  "Pellingham Test Services (final test)"]),
        ("Halcyon Microsystems", ["Sable Dynamics (DRAM and controller IP)",
                                  "Vorsa Thermal (thermal solutions)"]),
        ("Tessara Graphics Corporation", ["Lumenstor Memory (graphics memory)",
                                          "Calderway Substrates (organic substrate)"]),
        ("Brightfell Graphics", ["Arcwell Semiconductor (embedded graphics memory)"]),
        ("Cindermere Visual Systems", ["Lumenstor Memory (graphics memory)"]),
    ]
    for i, (co, sups) in enumerate(sibling_filings, start=1):
        listing = "\n".join("          - %s" % s for s in sups)
        doc(
            "SCD-%03d" % i,
            "%s statutory supply chain disclosure 2026" % co,
            "2026-%02d-%02d" % (rng.randint(4, 7), rng.randint(1, 28)),
            "%s regulatory filings" % co,
            "official_record",
            """
            This disclosure is published by %s in accordance with its statutory supply
            chain transparency obligations and covers the financial year ended
            31 December 2025.

            Section 1. Scope and governance. The company maintains a supplier
            classification framework and applies its responsible sourcing code to all
            direct production material suppliers. Governance sits with the procurement
            committee, which reports to the audit committee twice yearly.

            Section 2. Principal suppliers above the disclosure threshold. The company
            discloses direct production material suppliers individually accounting for
            more than ten per cent of production material spend in the period. Those
            suppliers were:

%s

            This is a threshold disclosure and not a complete list of the company's
            direct suppliers. Suppliers below the threshold are not named individually
            and their omission from this section carries no implication as to whether a
            commercial relationship exists.

            Section 3. Conflict minerals. Reasonable country of origin enquiry was
            conducted across the direct base. The company identified no smelter of
            concern in the tantalum stream. Work continues on the tin stream, where two
            smelters remain outside the conformant list and are subject to corrective
            action plans with quarterly review.

            Section 4. Labour standards. Supplier audits were completed at a majority
            of direct production sites. Non-conformities raised were classified minor
            and related principally to working hours records, dormitory standards and
            the storage of personal protective equipment. All were closed within the
            period or carried a time-bound remediation plan.

            Section 5. Grievance mechanism. An externally hosted grievance line
            operates in multiple languages and is available to supplier workers. Reports
            received in the period were triaged within five working days. None was
            substantiated as relating to forced labour.

            Section 6. Climate related supplier engagement. The company engaged its
            largest suppliers by spend on scope 1 and scope 2 reduction targets, and
            continues to treat purchased goods and services as the dominant and least
            certain component of its scope 3 inventory.

            Section 7. Water and waste. Direct operations are predominantly fabless and
            water withdrawal is therefore immaterial at the company level. Supplier
            level water risk is assessed for sites in basins classified as high stress.

            Section 8. Approval. This disclosure was approved by the board and signed on
            its behalf by a duly authorised officer.
            """
            % (co, listing)
            + "\n\n"
            + about_block([co]),
            tags=["sibling_filing"],
        )

    # ----------------------------- noise ------------------------------------------
    all_companies = (
        TOP5_GPU
        + OTHER_GPU
        + [SUPPLIER_A]
        + OTHER_SUPPLIERS
        + NON_GPU_CUSTOMERS
        + list(NEAR_DUPES)
    )
    first_names = "Imogen Pell Ravi Søren Marta Dele Yuki Aoife Tomas Nadia Bram Lior".split()
    last_names = "Rask Anwar Okonjo Lindqvist Berg Adeyemi Tanaka Doyle Varga Haddad Veld Katz".split()
    roles = [
        "chief executive",
        "chief financial officer",
        "senior vice president of engineering",
        "head of product",
        "chief operating officer",
        "vice president of operations",
    ]
    noise_kinds = [
        (
            "financial_results",
            "{c} reports {q} results",
            "{c} today reported results for {q}. Revenue was {rev} million, "
            "{dir} {pct} per cent year on year. Gross margin was {gm} per cent. "
            "\"{quote}\" said {person}, {role} at {c}. The company reiterated its "
            "full year outlook.",
        ),
        (
            "product_launch",
            "{c} introduces {prod}",
            "{c} today introduced {prod}, extending its product range. {prod} is "
            "sampling now with general availability expected in the coming quarters. "
            "\"{quote}\" said {person}, {role} at {c}.",
        ),
        (
            "exec_appointment",
            "{c} appoints {person} as {role}",
            "{c} today announced the appointment of {person} as {role}, effective "
            "immediately. {person} joins from a competing semiconductor firm and will "
            "report to the chief executive.",
        ),
        (
            "facility",
            "{c} expands {site} site",
            "{c} today announced an expansion of its {site} site, adding engineering "
            "and validation capacity. The investment is expected to create additional "
            "roles over the next two years. Construction begins next quarter.",
        ),
        (
            "sustainability",
            "{c} publishes annual environmental report",
            "{c} has published its annual environmental report. The company reported a "
            "reduction in scope 2 emissions and set an interim target for its purchased "
            "goods and services category. Supplier engagement continues across the "
            "production material base.",
        ),
        (
            "award",
            "{c} recognised at industry awards",
            "{c} was recognised in the engineering category at this year's industry "
            "awards. The citation referenced the company's validation methodology. "
            "\"{quote}\" said {person}, {role} at {c}.",
        ),
        (
            "partnership_nonmemory",
            "{c} partners with {other} on software tooling",
            "{c} and {other} today announced a collaboration on developer tooling and "
            "compiler support. The collaboration covers software only and involves no "
            "component supply arrangement between the parties.",
        ),
        (
            "pricing",
            "{c} adjusts channel pricing",
            "{c} has notified channel partners of a pricing adjustment across part of "
            "its range, citing substrate and test cost movements. The adjustment takes "
            "effect at the start of next quarter.",
        ),
        (
            "roadmap",
            "{c} outlines multi-year roadmap at investor day",
            "{c} set out a multi-year product roadmap at its investor day, covering "
            "process node transitions, packaging direction and software investment. "
            "The company did not name component suppliers for any future product. "
            "\"{quote}\" said {person}, {role} at {c}.",
        ),
        (
            "recall",
            "{c} issues service advisory for {prod}",
            "{c} has issued a service advisory covering a limited number of {prod} "
            "units affected by a board-level component tolerance issue. Affected units "
            "will be replaced. The company said the issue is contained and does not "
            "relate to memory components.",
        ),
    ]
    products = [
        "the L40X accelerator board",
        "the Lanthorn X2 series",
        "the Aurora M module",
        "the Orbitine V9 card",
        "the HM-500 co-packaged module",
        "the H-series Pro edition",
        "the KM-7 low-power family",
        "the GS-220 graphics module",
    ]
    sites = ["Dresden", "Hsinchu", "Austin", "Leixlip", "Penang", "Grenoble", "Bristol"]
    quotes = [
        "Execution in the period was consistent with the plan we set out",
        "Demand signals remain ahead of our installed capacity",
        "We are investing where the engineering constraint actually is",
        "Customers are planning further out than they were two years ago",
        "Packaging, not lithography, is where the cycle time now sits",
        "We would rather be disciplined on mix than chase unit share",
    ]

    n_noise = 90
    for i in range(n_noise):
        kind, title_t, body_t = rng.choice(noise_kinds)
        c = rng.choice(all_companies)
        other = rng.choice([x for x in all_companies if x != c])
        person = "%s %s" % (rng.choice(first_names), rng.choice(last_names))
        fields = {
            "c": c,
            "other": other,
            "person": person,
            "role": rng.choice(roles),
            "prod": rng.choice(products),
            "site": rng.choice(sites),
            "quote": rng.choice(quotes),
            "q": "the %s quarter" % rng.choice(["first", "second", "third", "fourth"]),
            "rev": rng.randint(180, 4200),
            "dir": rng.choice(["up", "down"]),
            "pct": rng.randint(2, 48),
            "gm": rng.randint(31, 66),
        }
        body = body_t.format(**fields)
        names = [c] + ([other] if "{other}" in body_t else [])
        body = body + "\n\n" + about_block(names)
        body = pad(body, rng, rng.choice([0, 0, 1, 1, 2, 3, 4, 5, 6]))
        doc(
            "NOISE-%03d" % (i + 1),
            title_t.format(**fields),
            "2026-%02d-%02d" % (rng.randint(1, 10), rng.randint(1, 28)),
            "%s newsroom" % c,
            kind,
            body,
            tags=["noise"],
        )

    return list(DOCS)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--truth", default=None)
    ap.add_argument("--seed", type=int, default=20261029)
    ap.add_argument("--no-aggregate", action="store_true")
    ap.add_argument("--two-zero", action="store_true",
                    help="remove Halcyon's evidence: TWO producers at zero evidence")
    ap.add_argument("--claim-n", type=int, default=4,
                    help="the N in the 'N of five' aggregate claim")
    ap.add_argument("--veradyne-evidence", action="store_true",
                    help="plant real supply evidence for Veradyne too (unsatisfiable)")
    a = ap.parse_args()

    rng = random.Random(a.seed)
    docs = build(rng, include_aggregate=not a.no_aggregate,
                 two_zero=a.two_zero, claim_n=a.claim_n,
                 veradyne_evidence=a.veradyne_evidence)

    os.makedirs(a.out, exist_ok=True)
    txt_dir = os.path.join(a.out, "docs")
    os.makedirs(txt_dir, exist_ok=True)
    # Clear stale .txt files from a previous generation with different settings,
    # so corpus/docs/ always matches corpus.json exactly.
    for _old in glob.glob(os.path.join(txt_dir, "*.txt")):
        os.remove(_old)
    public = []
    for d in docs:
        pub = {k: v for k, v in d.items() if not k.startswith("_")}
        public.append(pub)
        with open(os.path.join(txt_dir, d["id"] + ".txt"), "w") as f:
            f.write("TITLE: %s\nDATE: %s\nSOURCE: %s\nTYPE: %s\n\n%s\n"
                    % (d["title"], d["date"], d["source"], d["type"], d["text"]))
    with open(os.path.join(a.out, "corpus.json"), "w") as f:
        json.dump(public, f, indent=1)

    lens = sorted(len(d["text"].split()) for d in docs)
    print("docs=%d  words: min=%d median=%d max=%d total=%d"
          % (len(docs), lens[0], lens[len(lens) // 2], lens[-1], sum(lens)))

    if a.truth:
        os.makedirs(a.truth, exist_ok=True)
        truth = {
            "question": "Which four of the world's five top GPU producers does "
                        "Kestrel Memory Works supply memory to, and which one is excluded?",
            "supplier_a": SUPPLIER_A,
            "top5_gpu_producers": TOP5_GPU,
            "supplied": SUPPLIED,
            "excluded": EXCLUDED,
            "evidence_strength": {
                "Tessara Graphics Corporation": "explicit joint press release (TSG-2026-031)",
                "Orbitine Compute AB": "named supplier of record in a long statutory filing (ORB-SCD-2026)",
                "Nyxvolt Systems Inc.": "3-document chain, never jointly stated "
                                        "(KMW-2026-009 + NVS-2026-022 + NVS-JOB-4471)",
                "Halcyon Microsystems": "alias in earnings transcript (KMW-CALL-Q3) + "
                                        "joint conference session (GMS-2026-AGENDA)",
                "Veradyne Silicon N.V.": "none; only decoys",
            },
            "decoys": [
                "LIT-2026-0041", "LIT-2026-0088", "OMSC-2026-07",
                "SAE-2026-02", "NXL-2026-005",
            ],
            "near_duplicate_names": NEAR_DUPES,
            "non_gpu_kestrel_customers": NON_GPU_CUSTOMERS,
            "aggregate_claim_doc": None if a.no_aggregate else "KMW-2026-014",
            "seed": a.seed,
            "variant": {
                "two_zero": a.two_zero,
                "claim_n": None if a.no_aggregate else a.claim_n,
                "veradyne_evidence": a.veradyne_evidence,
            },
        }
        # What the evidence in THIS corpus can actually support, which is not
        # always the same as who is really supplied. With two zero-evidence
        # producers the corpus cannot single one out, and the honest expected
        # output is "ambiguous between those two".
        zero_ev = ["Veradyne Silicon N.V."]
        if a.two_zero:
            zero_ev.append("Halcyon Microsystems")
        if a.veradyne_evidence:
            zero_ev.remove("Veradyne Silicon N.V.")
        with_ev = [g for g in TOP5_GPU if g not in zero_ev]
        n_excluded = None if a.no_aggregate else (5 - a.claim_n)
        if a.no_aggregate:
            state = "ambiguous" if len(zero_ev) != 1 else "unique"
        elif len(with_ev) > a.claim_n:
            state = "unsatisfiable"
        elif len(zero_ev) == n_excluded:
            state = "unique"
        else:
            state = "ambiguous"
        truth["evidence_resolvability"] = {
            "producers_with_extracted_evidence": with_ev,
            "producers_with_zero_evidence": zero_ev,
            "n_excluded_implied_by_claim": n_excluded,
            "expected_state": state,
            "expected_excluded_set":
                zero_ev if state == "unique" else None,
            "expected_ambiguous_pool":
                zero_ev if state == "ambiguous" else None,
            "why": {
                "unique": "the number of zero-evidence producers equals the "
                          "number the claim says are excluded",
                "ambiguous": "more producers have zero evidence than the claim "
                             "excludes, so the count narrows the space without "
                             "closing it",
                "unsatisfiable": "more producers carry evidence than the claim "
                                 "allows to be supplied",
            }[state],
        }
        with open(os.path.join(a.truth, "ground_truth.json"), "w") as f:
            json.dump(truth, f, indent=1)
        # a tag map, for auditing only
        with open(os.path.join(a.truth, "doc_tags.json"), "w") as f:
            json.dump({d["id"]: d["_tags"] for d in docs}, f, indent=1)


if __name__ == "__main__":
    main()
